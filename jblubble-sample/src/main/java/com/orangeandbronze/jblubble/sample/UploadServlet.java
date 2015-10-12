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
import java.util.LinkedList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.orangeandbronze.jblubble.BlobInfo;
import com.orangeandbronze.jblubble.BlobKey;
import com.orangeandbronze.jblubble.BlobstoreService;

@WebServlet(value=UploadServlet.PATH + "/*", name="uploads-servlet")
@MultipartConfig
@SuppressWarnings("serial")
public class UploadServlet extends HttpServlet {

	public static final String PATH = "/uploads";

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	private WebApplicationContext applicationContext;
	private BlobstoreService blobstoreService;
	private List<BlobKey> blobKeys;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		applicationContext = WebApplicationContextUtils.getWebApplicationContext(
				config.getServletContext());
		blobstoreService = applicationContext.getBean(BlobstoreService.class);
		blobKeys = new LinkedList<>();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null) {
			pathInfo = "/";
		}
		LOGGER.debug("GET {}{}", PATH, pathInfo);
		if ("/create".equals(pathInfo)) {
			RequestDispatcher requestDispatcher =
					request.getRequestDispatcher(
							"/WEB-INF/views/uploads/create.jsp");
			requestDispatcher.forward(request, response);
		} else if (pathInfo.length() > 1) {
			BlobKey blobKey = new BlobKey(pathInfo.substring(1));
			BlobInfo blobInfo = blobstoreService.getBlobInfo(blobKey);
			response.setContentType(blobInfo.getContentType());
			// In Servlet API 3.1, use #setContentLengthLong(long)
			response.setContentLength((int) blobInfo.getSize());
			response.setDateHeader(
					"Last-Modified", blobInfo.getDateCreated().getTime());
			blobstoreService.serveBlob(blobKey, response.getOutputStream());
		} else {
			// else show links to blobs that were previously uploaded (if any)
			RequestDispatcher requestDispatcher =
					request.getRequestDispatcher(
							"/WEB-INF/views/uploads/index.jsp");
			List<BlobInfo> blobInfos = new LinkedList<>();
			for (BlobKey blobKey : blobKeys) {
				blobInfos.add(blobstoreService.getBlobInfo(blobKey));
			}
			request.setAttribute("blobstoreService", blobstoreService);
			request.setAttribute("blobKeys", blobKeys);
			request.setAttribute("blobInfos", blobInfos);
			requestDispatcher.forward(request, response);
		}
	}

	@Override
	protected long getLastModified(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null) {
			pathInfo = "/";
		}
		if (!"/create".equals(pathInfo) && pathInfo.length() > 1) {
			LOGGER.debug("GET Last-Modified {}{}", PATH, pathInfo);
			BlobKey blobKey = new BlobKey(pathInfo.substring(1));
			BlobInfo blobInfo = blobstoreService.getBlobInfo(blobKey);
			return blobInfo.getDateCreated().getTime();
		}
		return super.getLastModified(request);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null) {
			pathInfo = "/";
		}
		LOGGER.debug("POST {}{}", PATH, pathInfo);
		Part part = request.getPart("file");
		if (part != null) {
			BlobKey blobKey = blobstoreService.createBlob(
					part.getInputStream(),
					getFileName(part),
					part.getContentType());
			LOGGER.debug("Created blob, generated key [{}]", blobKey);
			blobKeys.add(blobKey);
		}
		response.sendRedirect(
				request.getServletContext().getContextPath() + "/uploads");
	}

	private String getFileName(Part part) {
		for (String cd : part.getHeader("content-disposition").split(";")) {
			if (cd.trim().startsWith("filename")) {
				return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}

}
