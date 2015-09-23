package jblubble.sample;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import jblubble.BlobInfo;
import jblubble.BlobstoreException;
import jblubble.BlobstoreService;

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

	protected Person getPersonById(String id) {
		try {
			Long personId = Long.valueOf(id);
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving Person with id=[{}]", personId);
			}
			return entityManager.find(Person.class, personId);
		} catch (NumberFormatException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Invalid number format: {}", id);
			}
		}
		return null;
	}

	protected List<Person> getAllPersons() {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Person> criteriaQuery = criteriaBuilder.createQuery(Person.class);
		criteriaQuery.select(criteriaQuery.from(Person.class));
		return entityManager.createQuery(criteriaQuery).getResultList();
	}

	protected String createPhotoBlob(MultipartFile photoFile) {
		String blobKey = null;
		if (photoFile.isEmpty()) {
			blobKey = null;
		} else {
			try {
				InputStream inputStream = photoFile.getInputStream();
				try {
					blobKey = blobstoreService.createBlob(
							inputStream,
							photoFile.getOriginalFilename(),
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

	@RequestMapping(method=RequestMethod.GET)
	public String list(Model model) {
		model.addAttribute("persons", getAllPersons());
		return PATH + "/list";
	}

	@RequestMapping(method=RequestMethod.GET, value="/{id}")
	public String show(@PathVariable("id") String id, Model model) {
		Person person = getPersonById(id);
		if (person == null) {
			// TODO Return a 404
		}
		model.addAttribute("person", person);
		return PATH + "/show";
	}

	@RequestMapping(method=RequestMethod.GET, value="/create")
	public String create(Model model) {
		model.addAttribute("person", new Person());
		return PATH + "/create";
	}

	@RequestMapping(method=RequestMethod.POST)
	public String save(
			@ModelAttribute Person person,
			@RequestParam("photo") MultipartFile photoFile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Saving {}", person);
		}
		String blobKey = createPhotoBlob(photoFile);
		person.setPhotoId(blobKey);
		entityManager.persist(person);
		return "redirect:" + PATH;
	}

	@RequestMapping(method=RequestMethod.GET, value="/{id}/edit")
	public String edit(@PathVariable("id") String id, Model model) {
		Person person = getPersonById(id);
		if (person == null) {
			// TODO Return a 404
		}
		model.addAttribute("person", person);
		return PATH + "/edit";
	}

	@RequestMapping(method=RequestMethod.PUT, value="/{id}")
	public String update(
			@PathVariable("id") String id,
			@ModelAttribute Person updatedPerson,
			@RequestParam("photo") MultipartFile photoFile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Saving {}", updatedPerson);
		}
		Person existingPerson = getPersonById(id);
		// Merge updatedPerson with existingPerson
		existingPerson.setName(updatedPerson.getName());
		String blobKey = createPhotoBlob(photoFile);
		if (existingPerson.getPhotoId() != null) {
			blobstoreService.delete(existingPerson.getPhotoId());
		}
		existingPerson.setPhotoId(blobKey);
		// Changes to existingPerson will automatically be persisted
		return "redirect:" + PATH;
	}

	@RequestMapping(method=RequestMethod.GET, value="/{id}/photo")
	public void servePhoto(
			@PathVariable("id") String id,
			WebRequest webRequest,
			HttpServletResponse response)
			throws BlobstoreException, IOException {
		Person person = getPersonById(id);
		if (person != null) {
			String photoId = person.getPhotoId();
			if (photoId != null) {
				BlobInfo blobInfo = blobstoreService.getBlobInfo(photoId);
				if (webRequest.checkNotModified(blobInfo.getDateCreated().getTime())) {
					return;
				}
				response.setContentType(blobInfo.getContentType());
				// In Servlet API 3.1, use #setContentLengthLong(long)
				response.setContentLength((int) blobInfo.getSize());
				response.setDateHeader(
						"Last-Modified", blobInfo.getDateCreated().getTime());
				blobstoreService.serveBlob(
						photoId, response.getOutputStream());
				return;
			}
		}
		// return a 404
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

}
