package de.wasabibeans.framework.server.core.dto;

import java.io.Serializable;
import java.util.Date;

public class WasabiVersionDTO implements Serializable {

	private static final long serialVersionUID = -8682148008577725108L;

	private String label;
	private String creator;
	private String comment;
	private Date creationDate;

	protected WasabiVersionDTO(String label, String creator, String comment, Date creationDate) {
		this.label = label;
		this.comment = comment;
		this.creationDate = creationDate;
	}

	public String getLabel() {
		return label;
	}

	public String getCreator() {
		return creator;
	}

	public String getComment() {
		return comment;
	}

	public Date getCreationDate() {
		return creationDate;
	}
}
