package jblubble.sample;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import jblubble.BlobKey;

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
