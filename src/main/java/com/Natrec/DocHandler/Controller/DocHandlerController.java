package com.Natrec.DocHandler.Controller;

import com.Natrec.DocHandler.Model.ScanDoc;
import com.Natrec.DocHandler.Repository.ScanDocRepository;
import com.dynamsoft.dbr.*;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Controller
public class DocHandlerController {

	@Autowired
	ScanDocRepository docRepository;

	@ResponseBody
	@CrossOrigin(origins = "*")
	@RequestMapping("/doc")
	public void saveParent_file(@RequestParam("RemoteFile") MultipartFile files,@RequestParam Long Session_id)
			throws IOException, BarcodeReaderException {

		ScanDoc doc = new ScanDoc();
		doc.setDocFile(files.getBytes());
		Integer size = files.getBytes().length / 1024 * 2;
		doc.setFileSize(size.toString());
		doc.setNoPages(numOfPage(files));
		doc.setSessionId(Session_id);
		ScanDoc parent_id = docRepository.save(doc);

		System.err.println("num of pages is " + numOfPage(files));

		System.err.println(parent_id.getScanId());
		Read_QR_split_doc(parent_id.getScanId(), files,Session_id);

//		
//		
//		
//
//		String dictory = System.getProperty("user.dir");
//		File f = new File(dictory + "\\docs\\");
//		f.mkdirs();
//
//		if (f.exists()) {
//
//			Path fileNameAndPath = Paths.get(f.getPath(), files.getOriginalFilename());
//
//			System.out.println(fileNameAndPath.toAbsolutePath());
//			System.out.println("file readed Successfully");
//
//			try {
//				Files.write(fileNameAndPath, files.getBytes());
//				Read_QR_split_doc(fileNameAndPath);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}

	}

	private void Read_QR_split_doc( long parent_id, MultipartFile files,Long Session_id)
			throws BarcodeReaderException, IOException {

		BarcodeReader reader = new BarcodeReader();
		reader.initLicense(
				"t0073oQAAACeGM6PuW4Sc5NUiKHnQl8h/12Vl0o+pf7nxcVgxLpSkB+Bzhr72QEQwWdmQLWzlPjgZkYWt4LKNKgvDCFVGfJoPzmsiQg==");

		PublicRuntimeSettings runtimeSettings = reader.getRuntimeSettings();
		runtimeSettings.barcodeFormatIds = EnumBarcodeFormat.BF_ALL;
		runtimeSettings.barcodeFormatIds_2 = EnumBarcodeFormat_2.BF2_POSTALCODE | EnumBarcodeFormat_2.BF2_DOTCODE;
		runtimeSettings.expectedBarcodesCount = 1005;
		reader.updateRuntimeSettings(runtimeSettings);

		// File file = new File(path.toString());
		PDDocument doc = null;
		try {
			doc = PDDocument.load(files.getBytes());
		} catch (IOException e) {
			System.err.println("please set file as a pdf extention");
		}

		// Instantiating Splitter class
		Splitter splitter = new Splitter();

		// splitting the pages of a PDF document
		List<PDDocument> Pages = null;
		try {
			Pages = splitter.split(doc);
		} catch (IOException e) {
			System.err.println("please set file as a pdf extention");
		}

		// Creating an iterator
		Iterator<PDDocument> iterator = Pages.listIterator();

		// Saving each page as an individual document

		while (iterator.hasNext() /* && iIndex < results.length */) {

			ScanDoc docs = new ScanDoc();

			docs.setParentId(parent_id);
            docs.setSessionId(Session_id);
			PDDocument pd = iterator.next();
			docs.setNoPages(pd.getNumberOfPages());

			// save to file System

//			String dictory = System.getProperty("user.dir");
//			File f = new File(dictory + "\\docs\\separated_pdf\\");
//			f.mkdirs();
//			pd.save(f + "/" + "employee" + ".pdf");
			//////////

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			pd.save(out);
			docs.setDocFile(out.toByteArray());
			Integer s = out.toByteArray().length;
			docs.setFileSize(s.toString());

			List File_with_Barcode = new ArrayList();
			List File_NO_Barcode = new ArrayList();

			TextResult[] results;
			try {

				results = reader.decodeFileInMemory(out.toByteArray(), "");
				System.out.println("Total barcodes found: " + results.length);
				for (int iIndex = 0; iIndex < results.length; ++iIndex) {
					System.out.println("Barcode " + (iIndex + 1));
					if (results[iIndex].barcodeFormat != 0) {

						if (results[iIndex].barcodeFormatString.equals("QR_CODE")) {

							docs.setQrCode(results[iIndex].barcodeText);
							System.out.println("    Barcode Format: " + results[iIndex].barcodeFormatString);

						} else {
							File_with_Barcode.add(out.toByteArray());
							docs.setDocFileName(docRepository.getFileName(results[iIndex].barcodeText));
							docs.setBarCode(results[iIndex].barcodeText);
							System.out.println("    Barcode Format: " + results[iIndex].barcodeFormatString);
						}

					} else {
						System.out.println("    Barcode Format: " + results[iIndex].barcodeFormatString_2);
					}
					System.out.println("    Barcode Text: " + results[iIndex].barcodeText);

				}

			} catch (BarcodeReaderException exp) {
				System.err.println(exp.getMessage());
			}

			docRepository.save(docs);
			pd.close();

			// Loading an existing PDF document

		}
		reader.destroy();
		System.out.println("PDF splitted");
	}

	@ResponseBody
	@RequestMapping("/mos")
	public String test() throws IOException {

		File file = new File("E:\\output (17).pdf");
		byte[] bytes = FileUtils.readFileToByteArray(file);

		ScanDoc docs = new ScanDoc();
		docs.setFileSize("99");
		docs.setBarCode("BB");
		docs.setQrCode("TT");
		docs.setDocFile(bytes);
		docs.setParentId(55555);

		docRepository.save(docs);
		return "done";
	}

	@ResponseBody
	@RequestMapping("/mos2")
	public String test2() {
		Optional<ScanDoc> docs = docRepository.findById((long) 249001);
		ScanDoc doc = docs.get();

		try {

			OutputStream os = new FileOutputStream(new File("mostafa.pdf"));

			os.write(doc.getDocFile());

			System.out.println("Successfully" + " byte inserted");

			os.close();
		}

		catch (Exception e) {

			System.out.println("Exception: " + e);
		}

		System.err.println(doc.getQrCode());
		return "done2";
	}

	@ResponseBody
	@RequestMapping("/te")
	public String test44() {
		String s5 = docRepository.getFileName("mostafa");
		System.err.println(s5);
		return "ok";

	}

	public int numOfPage(MultipartFile files) throws IOException {
		int numberOfPages = 0;
		InputStream file = files.getInputStream();
		PDDocument document = PDDocument.load(file);

		if (document != null) {
			numberOfPages = document.getNumberOfPages();
		}
		return numberOfPages;
	}

}
