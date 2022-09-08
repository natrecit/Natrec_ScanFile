package com.Natrec.DocHandler.Controller;

import com.Natrec.DocHandler.Model.ScanDoc;
import com.Natrec.DocHandler.Repository.ScanDocRepository;
import com.Natrec.DocHandler.Utils.Page;
import com.dynamsoft.dbr.*;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	public void saveParent_file(@RequestParam("RemoteFile") MultipartFile files, @RequestParam Long Session_id)
			throws IOException, BarcodeReaderException {
		String parent_name = docRepository.getFileName("Parent") + ".pdf";

		ScanDoc doc = new ScanDoc();
		doc.setDocFileName(parent_name);
		doc.setDocFile(files.getBytes());
		Integer size = files.getBytes().length;
		doc.setFileSize(size.toString());
		doc.setNoPages(numOfPage(files.getBytes()));
		doc.setSessionId(Session_id);
		ScanDoc parent_id = docRepository.save(doc);

		System.err.println("num of pages is " + numOfPage(files.getBytes()));

//		System.err.println(parent_id.getScanId());
		Read_QR_split_doc(parent_id.getScanId(), files, Session_id);

		String dictory = System.getProperty("user.dir");
		File f = new File(dictory + "\\docs\\");
		f.mkdirs();

		if (f.exists()) {

			Path fileNameAndPath = Paths.get(f.getPath(), parent_name);

			System.out.println(fileNameAndPath.toAbsolutePath());
			System.out.println("file readed Successfully");

			try {
				Files.write(fileNameAndPath, files.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void Read_QR_split_doc(long parent_id, MultipartFile files, Long Session_id)
			throws BarcodeReaderException, IOException {

		BarcodeReader reader = new BarcodeReader();
		reader.initLicense(
				"t0073oQAAACeGM6PuW4Sc5NUiKHnQl8h/12Vl0o+pf7nxcVgxLpSkB+Bzhr72QEQwWdmQLWzlPjgZkYWt4LKNKgvDCFVGfJoPzmsiQg==");

		PublicRuntimeSettings runtimeSettings = reader.getRuntimeSettings();
		runtimeSettings.barcodeFormatIds = EnumBarcodeFormat.BF_ALL;
		runtimeSettings.barcodeFormatIds_2 = EnumBarcodeFormat_2.BF2_POSTALCODE | EnumBarcodeFormat_2.BF2_DOTCODE;
		runtimeSettings.expectedBarcodesCount = 1005;
		reader.updateRuntimeSettings(runtimeSettings);

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
		List<Page> File_with_Barcode = new ArrayList<Page>();
		List<Page> File_NO_Barcode = new ArrayList<Page>();
		while (iterator.hasNext() /* && iIndex < results.length */) {

			PDDocument pd = iterator.next();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			pd.save(out);

			TextResult[] results;
			try {
				Page p = new Page();
				results = reader.decodeFileInMemory(out.toByteArray(), "");
				System.out.println("Total barcodes found: " + results.length);

				if (results.length == 0) {
					p.setFile(out.toByteArray());
					p.setBarCode(null);
					p.setQrCode(null);
					File_NO_Barcode.add(p);
				} else {
					for (int iIndex = 0; iIndex < results.length; ++iIndex) {
						System.out.println("Barcode " + (iIndex + 1));

						if (results[iIndex].barcodeFormatString.equals("QR_CODE")) {

							p.setQrCode(results[iIndex].barcodeText);

							System.out.println("    Barcode Format: " + results[iIndex].barcodeFormatString);

						} else {

							p.setBarCode(results[iIndex].barcodeText);

							System.out.println("    Barcode Format: " + results[iIndex].barcodeFormatString);
						}

						System.out.println("    Barcode Text: " + results[iIndex].barcodeText);
					}
					p.setFile(out.toByteArray());
					File_with_Barcode.add(p);
				}
			}

			catch (BarcodeReaderException exp) {
				System.err.println(exp.getMessage());
			}
			

			pd.close();

			// Loading an existing PDF document

		}
		sort(File_with_Barcode);

		Create_PDf_Wish_Same_Barcode(File_with_Barcode, parent_id, Session_id);
		Create_PDf_Without_Barcode(File_NO_Barcode, parent_id, Session_id);
		reader.destroy();
		System.out.println("PDF splitted");
	}

	private void Create_PDf_Wish_Same_Barcode(List<Page> File_with_Barcode, long parent_id, long Session_id)
			throws IOException {

		String barCode = "";
		String QRCode = "";

		List<InputStream> temp = new ArrayList<InputStream>();

		for (int i = 0; i < File_with_Barcode.size(); i++) {

			for (int j = i; j < File_with_Barcode.size(); j++) {

				if (j == File_with_Barcode.size() - 1) {
					i = j;
				}

				if (File_with_Barcode.get(i).getBarCode().equals(File_with_Barcode.get(j).getBarCode())) {

					temp.add(new ByteArrayInputStream(File_with_Barcode.get(j).getFile()));
					barCode = File_with_Barcode.get(j).getBarCode();
					QRCode = File_with_Barcode.get(j).getQrCode();
				} else {
					i = j - 1;

					break;
				}
			}

			String dictory = System.getProperty("user.dir");
			File f = new File(dictory + "\\Docs\\Splitted_pdf\\");
			f.mkdirs();

			String FileName = docRepository.getFileName(barCode);

			PDFMergerUtility ut = new PDFMergerUtility();
			ut.addSources(temp);
			ut.setDestinationFileName(f + "/" + FileName + ".pdf");

			ut.mergeDocuments();

			File f2 = new File(f + "/" + FileName + ".pdf");

			byte[] bytes = FileUtils.readFileToByteArray(f2);
			Integer fileSize = bytes.length;
			ScanDoc scanDocs = new ScanDoc();
			scanDocs.setParentId(parent_id);
			scanDocs.setSessionId(Session_id);
			scanDocs.setDocFileName(FileName);
			scanDocs.setDocFile(bytes);
			scanDocs.setFileSize(fileSize.toString());
			scanDocs.setBarCode(barCode);
			scanDocs.setQrCode(QRCode);
			scanDocs.setNoPages(numOfPage(bytes));
			docRepository.save(scanDocs);
			barCode = "";
			QRCode = "";
			temp.clear();

		}

	}

	private void Create_PDf_Without_Barcode(List<Page> File_without_Barcode, long parent_id, long Session_id)
			throws IOException {
		List<InputStream> temp = new ArrayList<InputStream>();

		for (int i = 0; i < File_without_Barcode.size(); i++) {
			temp.add(new ByteArrayInputStream(File_without_Barcode.get(i).getFile()));
		}

		String dictory = System.getProperty("user.dir");
		File f = new File(dictory + "\\Docs\\Splitted_pdf\\");
		f.mkdirs();

		PDFMergerUtility ut = new PDFMergerUtility();
		ut.addSources(temp);
		ut.setDestinationFileName(f + "/" + "NoBarCode.pdf");

		ut.mergeDocuments();

		String FileName = docRepository.getFileName("NoBarCode");
		File f2 = new File(f + "/" + FileName + ".pdf");

		byte[] bytes = FileUtils.readFileToByteArray(f2);
		Integer fileSize = bytes.length;
		ScanDoc scanDocs = new ScanDoc();
		scanDocs.setParentId(parent_id);
		scanDocs.setSessionId(Session_id);
		scanDocs.setDocFileName(FileName);
		scanDocs.setNoBarcode(bytes);
		scanDocs.setFileSize(fileSize.toString());
		scanDocs.setNoPages(numOfPage(bytes));
		docRepository.save(scanDocs);

	}

	public int numOfPage(byte[] files) throws IOException {
		int numberOfPages = 0;
		InputStream file = new ByteArrayInputStream(files);
		PDDocument document = PDDocument.load(file);

		if (document != null) {
			numberOfPages = document.getNumberOfPages();
		}
		return numberOfPages;
	}

	private void sort(List<Page> list) {

		list.sort((o1, o2) -> o1.getBarCode().compareTo(o2.getBarCode()));

	}

//	
//	@ResponseBody
//	@RequestMapping("/mos")
//	public String test() throws IOException {
//
//		File file = new File("E:\\output (17).pdf");
//		byte[] bytes = FileUtils.readFileToByteArray(file);
//
//		ScanDoc docs = new ScanDoc();
//		docs.setFileSize("99");
//		docs.setBarCode("BB");
//		docs.setQrCode("TT");
//		docs.setDocFile(bytes);
//		docs.setParentId(55555);
//
//		docRepository.save(docs);
//		return "done";
//	}
//
//	@ResponseBody
//	@RequestMapping("/mos2")
//	public String test2() {
//		Optional<ScanDoc> docs = docRepository.findById((long) 249001);
//		ScanDoc doc = docs.get();
//
//		try {
//
//			OutputStream os = new FileOutputStream(new File("mostafa.pdf"));
//
//			os.write(doc.getDocFile());
//
//			System.out.println("Successfully" + " byte inserted");
//
//			os.close();
//		}
//
//		catch (Exception e) {
//
//			System.out.println("Exception: " + e);
//		}
//
//		System.err.println(doc.getQrCode());
//		return "done2";
//	}
//
//	@ResponseBody
//	@RequestMapping("/te")
//	public String test44() {
//		String s5 = docRepository.getFileName("mostafa");
//		System.err.println(s5);
//		return "ok";
//
//	}

}
