package com.Natrec.DocHandler.Model;



import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "SM_SCAN_DOC")
public class ScanDoc {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "SCAN_DOC_ID ")
	private long scanId;
	
	
	@Column(name = "PARENT_DOC_ID" )
	private long parentId;
	
	@Column(name = "BARCODE")
	private String barCode;
	
	@Column(name = "QRCODE")
	private String qrCode;
	
	@Column(name = "DOC_FILE_NAME")
	private String docFileName;
	
	@Lob
	@Column(name = "DOC_FILE")
	private byte[] docFile;;
	
	@Column(name = "FILE_SIZE")
	private String fileSize;
	
	
	@Column(name = "NO_BARCODE")
	private byte[] NoBarcode;
	
	@Column(name = "NO_OF_PAGES")
	private long noPages;
	
	@Column(name = "SESSION_ID")
	private long sessionId;
	
}
