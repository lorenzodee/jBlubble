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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.orangeandbronze.jblubble.BlobKey;

@Entity
public class Person {
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	private String name;
	@Column(name="photo_id")
	@Convert(converter=BlobKeyConverter.class)
	private BlobKey photoId;

	public Person() {}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BlobKey getPhotoId() {
		return photoId;
	}

	public void setPhotoId(BlobKey photoId) {
		this.photoId = photoId;
	}

	@Override
	public String toString() {
		return "Person [id=" + id + ", name=" + name + ", photoId=" + photoId + "]";
	}

}
