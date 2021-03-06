/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orangeandbronze.jblubble.sample;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import com.orangeandbronze.jblubble.BlobInfo;
import com.orangeandbronze.jblubble.BlobKey;
import com.orangeandbronze.jblubble.BlobstoreException;
import com.orangeandbronze.jblubble.BlobstoreService;

@RequestMapping("/" + PersonController.PATH)
@Controller
@Transactional
public class PersonController {

	public static final String PATH = "persons";

	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private BlobstoreService blobstoreService;

	// Ideally, persistence should NOT be part of the presentation layer.
	// But since this is just an example, we've simplified things here.
	// private PersonService service;

	/**
	 * Retrieves the person with the given id. Throwing an
	 * IllegalArgumentException if there is no such person.
	 * 
	 * @param id
	 *            the given id
	 * @return the person with the given id
	 */
	protected Person getPersonById(String id) {
		Person person = null;
		try {
			Long personId = Long.valueOf(id);
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving Person with id=[{}]", personId);
			}
			person = entityManager.find(Person.class, personId);
		} catch (NumberFormatException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Invalid number format: {}", id);
			}
		}
		if (person == null) {
			throw new IllegalArgumentException();
		}
		return person;
	}

	protected List<Person> getAllPersons() {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Person> criteriaQuery = criteriaBuilder.createQuery(Person.class);
		criteriaQuery.select(criteriaQuery.from(Person.class));
		return entityManager.createQuery(criteriaQuery).getResultList();
	}

	protected BlobKey createPhotoBlob(MultipartFile photoFile) {
		BlobKey blobKey = null;
		if (photoFile.isEmpty()) {
			blobKey = null;
		} else {
			try {
				InputStream inputStream = photoFile.getInputStream();
				try {
					blobKey = blobstoreService.createBlob(inputStream, photoFile.getOriginalFilename(),
							photoFile.getContentType());
				} finally {
					inputStream.close();
				}
			} catch (Exception e) {
				logger.error("Error saving person's photo", e);
			}
		}
		return blobKey;
	}

	@RequestMapping(method = RequestMethod.GET)
	public String list(Model model) {
		model.addAttribute("persons", getAllPersons());
		return PATH + "/list";
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public String show(@PathVariable("id") String id, Model model) {
		Person person = getPersonById(id);
		model.addAttribute("person", person);
		return PATH + "/show";
	}

	@RequestMapping(method = RequestMethod.GET, value = "/create")
	public String create(Model model) {
		model.addAttribute("person", new Person());
		return PATH + "/create";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String save(@ModelAttribute Person person, @RequestParam("photo") MultipartFile photoFile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Saving {}", person);
		}
		BlobKey blobKey = createPhotoBlob(photoFile);
		person.setPhotoId(blobKey);
		entityManager.persist(person);
		return "redirect:" + PATH;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/edit")
	public String edit(@PathVariable("id") String id, Model model) {
		Person person = getPersonById(id);
		model.addAttribute("person", person);
		return PATH + "/edit";
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public String update(@PathVariable("id") String id, @ModelAttribute Person updatedPerson,
			@RequestParam("photo") MultipartFile photoFile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Saving {}", updatedPerson);
		}
		Person existingPerson = getPersonById(id);
		// Merge updatedPerson with existingPerson
		existingPerson.setName(updatedPerson.getName());
		BlobKey blobKey = createPhotoBlob(photoFile);
		if (existingPerson.getPhotoId() != null) {
			blobstoreService.delete(existingPerson.getPhotoId());
		}
		existingPerson.setPhotoId(blobKey);
		// Changes to existingPerson will automatically be persisted
		return "redirect:" + PATH;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/photo")
	public void servePhoto(@PathVariable("id") String id, WebRequest webRequest, HttpServletResponse response)
			throws BlobstoreException, IOException {
		Person person = getPersonById(id);
		if (person != null) {
			BlobKey photoId = person.getPhotoId();
			if (photoId != null) {
				BlobInfo blobInfo = blobstoreService.getBlobInfo(photoId);
				if (webRequest.checkNotModified(blobInfo.getDateCreated().getTime())) {
					return;
				}
				response.setContentType(blobInfo.getContentType());
				// In Servlet API 3.1, use #setContentLengthLong(long)
				response.setContentLength((int) blobInfo.getSize());
				response.setDateHeader("Last-Modified", blobInfo.getDateCreated().getTime());
				// response.addHeader("Cache-Control", "must-revalidate, max-age=3600");
				blobstoreService.serveBlob(photoId, response.getOutputStream());
				return;
			}
		}
		throw new IllegalArgumentException();
	}

	/**
	 * Maps IllegalArgumentExceptions to a 404 Not Found HTTP status code.
	 */
	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler({ IllegalArgumentException.class })
	public void handleNotFound() {
		// just return empty 404
	}

}
