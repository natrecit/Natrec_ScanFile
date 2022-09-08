package com.Natrec.DocHandler.Utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Page {
	private String QrCode;
	private String barCode;
	private byte file[];

}
