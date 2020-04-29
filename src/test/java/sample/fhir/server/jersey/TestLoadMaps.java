package sample.fhir.server.jersey;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.dstu3.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.dstu3.model.Enumerations.ConceptMapEquivalence;
import org.hl7.fhir.dstu3.model.UriType;
import org.junit.BeforeClass;
import org.junit.Test;

//import com.sun.java.util.jar.pack.Package.File;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
//import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
//import ca.uhn.fhir.rest.server.EncodingEnum;
import sample.fhir.server.jersey.provider.TerminologyUtil;

public class TestLoadMaps {

	// http://localhost:8080/fhir/ConceptMap/$translate?code=C0349375&source=xxxxxxxxxx&target=1111222233334444
	// VAVistA 2.16.840.1.113883.6.233 http://hl7.org/fhir/ValueSet/v3-ReligiousAffiliation

	// http://localhost:8080/fhir/ConceptMap/$translate?code=DIVINATION&source=VAVistA&target=http://hl7.org/fhir/ValueSet/v3-ReligiousAffiliation
	private static IGenericClient client;

	private static FhirContext ourCtx = FhirContext.forDstu3();

	private static int ourPort = 8080;

	private static String HOST = "http://localhost:";

	@BeforeClass
	public static void setUpClass() throws Exception {

		ourCtx.getRestfulClientFactory().setConnectTimeout(50000);
		ourCtx.getRestfulClientFactory().setSocketTimeout(10000000);
		client = ourCtx.newRestfulGenericClient("http://localhost:8180/terminology/fhir");
		client.setEncoding(EncodingEnum.JSON);
		client.registerInterceptor(new LoggingInterceptor(true));
	}

	public static List<String> loadMapsxx(String directory) {

		// FhirContext fhirContext = FhirContext.forDstu3();
		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory))) {
			for (Path path : directoryStream) {
				ConceptMap conceptMapFromTo = new ConceptMap();
				ConceptMap conceptMapToFrom = new ConceptMap();
				ConceptMapGroupComponent cmgcFromTo = conceptMapFromTo.addGroup();
				ConceptMapGroupComponent cmgcToFrom = conceptMapToFrom.addGroup();
				boolean firstLine = true;

				for (String line : Files.readAllLines(path)) {
					// String[] code2code = line.toString().split(",");
					String[] code2code = line.toString().split("\t");
					if (firstLine) {
						firstLine = false;

						if (code2code.length == 4) {

							UriType sourceuri = new UriType();
							sourceuri.setValue(code2code[0]);

							conceptMapFromTo.setSource(sourceuri);
							conceptMapToFrom.setTarget(sourceuri);

							UriType targeturi = new UriType();
							targeturi.setValue(code2code[2]);

							conceptMapFromTo.setTarget(targeturi);
							conceptMapToFrom.setSource(targeturi);

							cmgcFromTo.setSource(code2code[1]);
							cmgcFromTo.setTarget(code2code[3]);

							cmgcToFrom.setTarget(code2code[1]);
							cmgcToFrom.setSource(code2code[3]);

						} else {
							System.out.println("invalid " + line);
						}
					} else {
						if (code2code.length == 4) {

							SourceElementComponent secFromTo = cmgcFromTo.addElement();
							CodeType aaa = new CodeType();
							secFromTo.setCodeElement(aaa);
							secFromTo.setCode(code2code[0]).addTarget().setCode(code2code[2]).setEquivalence(
								ConceptMapEquivalence.EQUAL);

							SourceElementComponent secToFrom = cmgcToFrom.addElement();
							CodeType aaa2 = new CodeType();
							secToFrom.setCodeElement(aaa2);
							secToFrom.setCode(code2code[2]).addTarget().setCode(code2code[0]).setEquivalence(
								ConceptMapEquivalence.EQUAL);

						} else {
							System.out.println("invalid " + line);
						}
					}
				}

				System.out.println(
					"Appointment JSon::" +
							ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(conceptMapFromTo));

				client.setEncoding(EncodingEnum.JSON);
				final MethodOutcome results = client.create().resource(conceptMapFromTo).prefer(
					PreferReturnEnum.REPRESENTATION).execute();
				System.out.println(results.getId());
				client.create().resource(conceptMapToFrom).prefer(PreferReturnEnum.REPRESENTATION).execute();
			}
		} catch (IOException ex) {
		}
		return fileNames;
	}

	@Test
	public void loadFromMappings() throws IOException {
		TerminologyUtil.load(client, "src/test/resources/mappings/loinc");
	}

}
