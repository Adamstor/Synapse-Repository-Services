package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

import javassist.bytecode.ByteArray;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.im4java.core.ConvertCmd;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.util.TestStreams;
import org.springframework.util.StreamUtils;

public class PdfPreviewTest {
	private static final String TEST_PDF_NAME = "images/test.pdf";
	private static final String TEST_PDF_GIF = "images/testpdf_result.gif";

	private PdfPreviewGenerator pdfPreviewGenerator;

	@BeforeClass
	public static void beforeClass() throws IOException {
		ConvertCmd convert = new ConvertCmd();
		try {
			convert.searchForCmd(convert.getCommand().get(0), PdfPreviewGenerator.IMAGE_MAGICK_SEARCH_PATH);
		} catch (FileNotFoundException e) {
			Assume.assumeNoException(e);
		}
	}

	@Before
	public void before() throws IOException, ServiceUnavailableException {
		pdfPreviewGenerator = new PdfPreviewGenerator();
	}

	@Test
	public void testGeneratePreview() throws IOException {
		InputStream in = PdfPreviewGenerator.class.getClassLoader().getResourceAsStream(TEST_PDF_NAME);
		assertNotNull("Failed to find a test file on the classpath: " + TEST_PDF_NAME, in);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PreviewOutputMetadata metaData = pdfPreviewGenerator.generatePreview(in, baos);
		baos.close();
		assertEquals("image/gif", metaData.getContentType());
		assertEquals(".gif", metaData.getExtension());
		// File tmp = File.createTempFile("temp", ".gif");
		// FileOutputStream tmpOut = new FileOutputStream(tmp);
		// StreamUtils.copy(baos.toByteArray(), tmpOut);
		// tmpOut.close();
		// System.out.println(tmp.getAbsolutePath());

		InputStream expected = PdfPreviewGenerator.class.getClassLoader().getResourceAsStream(TEST_PDF_GIF);
		TestStreams.assertEquals(expected, new ByteArrayInputStream(baos.toByteArray()));
	}

	@Test
	@Ignore
	public void testMany() throws Exception {
		File dir = new File("C:/cygwin64/home/mblonk/tmp/pdf");
		for (File pdf : dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".pdf");
			}
		})) {
			File preview = new File(dir.getAbsolutePath().replace("\\pdf", "\\jpg"), pdf.getName().replace(".pdf", ".gif"));
			if (!preview.exists()) {
				FileOutputStream out = new FileOutputStream(preview);
				InputStream in = new BufferedInputStream(new FileInputStream(pdf));
				System.out.println(pdf.getPath());
				try {
					pdfPreviewGenerator.generatePreview(in, out);
				} catch (RuntimeException e) {
					System.err.println(e.getMessage());
					out.close();
					preview.delete();
				}
				in.close();
				out.close();
			}
		}
	}
}
