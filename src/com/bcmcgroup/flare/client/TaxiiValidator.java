package com.bcmcgroup.flare.client;

import org.apache.log4j.Logger;
import org.mitre.stix.validator.SchemaError;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class TaxiiValidator {
    private static final Logger logger = Logger.getLogger(TaxiiValidator.class);
    private Schema schema;

    public TaxiiValidator() throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schema = schemaFactory.newSchema(Paths.get("schemas/uber_schema.xsd").toFile());
    }

    public boolean validate(Document taxiiDoc) throws SAXException, IOException {

        Source source = new DOMSource(taxiiDoc);
        Validator validator = schema.newValidator();
        TaxiiErrorHandler errorHandler = new com.bcmcgroup.flare.client.TaxiiValidator.TaxiiErrorHandler();
        validator.setErrorHandler(errorHandler);

        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            logger.error("SAXParseException when attempting to validate a TAXII document.");
            return false;
        }

        List<SchemaError> errors = errorHandler.getErrors();
        if (errors.size() > 0) {
            for (SchemaError error : errors) {
                logger.error("Validation Schema Error Category: " + error.getCategory());
                logger.error("Validation Schema Error Message: " + error.getMessage());
            }
            logger.error("Message was not published due to TAXII validation errors.");
            return false;
        }

        logger.debug("TAXII Validation: No Errors");
        return true;
    }

    public class TaxiiErrorHandler implements ErrorHandler {
        final List<SchemaError> errors = new ArrayList<>();

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            errors.add(SchemaError.fromException(exception, SchemaError.Categories.WARNING));
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            errors.add(SchemaError.fromException(exception, SchemaError.Categories.ERROR));
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            errors.add(SchemaError.fromException(exception, SchemaError.Categories.FATAL_ERROR));
        }

        public List<SchemaError> getErrors() {
            return errors;
        }
    }
}