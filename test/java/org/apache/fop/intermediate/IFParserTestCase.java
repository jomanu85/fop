/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.intermediate;

import java.io.File;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFParser;
import org.apache.fop.render.intermediate.IFRenderer;
import org.apache.fop.render.intermediate.IFSerializer;

/**
 * Tests the intermediate format parser.
 */
public class IFParserTestCase extends AbstractIntermediateTestCase {

    /**
     * Constructor for the test suite that is used for each test file.
     * @param testFile the test file to run
     */
    public IFParserTestCase(File testFile) {
        super(testFile);
    }

    /** {@inheritDoc} */
    protected String getTargetMIME() {
        return MimeConstants.MIME_PDF + ";mode=painter";
    }

    /** {@inheritDoc} */
    protected String getIntermediateFileExtension() {
        return ".if.xml";
    }

    /** {@inheritDoc} */
    protected Document buildIntermediateDocument(Source src, Templates templates)
                throws Exception {
        Transformer transformer;
        if (templates != null) {
            transformer = templates.newTransformer();
        } else {
            transformer = tFactory.newTransformer();
        }

        setErrorListener(transformer);

        //Set up XMLRenderer to render to a DOM
        DOMResult domResult = new DOMResult();

        FOUserAgent userAgent = createUserAgent();

        //Create an instance of the target renderer so the XMLRenderer can use its font setup
        IFDocumentHandler targetHandler = userAgent.getRendererFactory().createDocumentHandler(
                userAgent, getTargetMIME());

        //Setup painter
        IFSerializer serializer = new IFSerializer();
        serializer.setUserAgent(userAgent);
        serializer.mimicDocumentHandler(targetHandler);
        serializer.setResult(domResult);

        //Setup renderer
        IFRenderer renderer = new IFRenderer();
        renderer.setUserAgent(userAgent);

        renderer.setDocumentHandler(serializer);
        userAgent.setRendererOverride(renderer);

        Fop fop = fopFactory.newFop(userAgent);
        Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);

        return (Document)domResult.getNode();
    }

    /** {@inheritDoc} */
    protected void parseAndRender(Source src, OutputStream out) throws Exception {
        IFParser parser = new IFParser();

        FOUserAgent userAgent = createUserAgent();

        IFDocumentHandler documentHandler = userAgent.getRendererFactory().createDocumentHandler(
                userAgent, getTargetMIME());
        documentHandler.setResult(new StreamResult(out));
        documentHandler.setDefaultFontInfo(new FontInfo());
        parser.parse(src, documentHandler, userAgent);
    }

    /** {@inheritDoc} */
    protected Document parseAndRenderToIntermediateFormat(Source src) throws Exception {
        IFParser parser = new IFParser();

        FOUserAgent userAgent = createUserAgent();

        IFSerializer serializer = new IFSerializer();
        serializer.setUserAgent(userAgent);
        DOMResult domResult = new DOMResult();
        serializer.setResult(domResult);

        parser.parse(src, serializer, userAgent);

        return (Document)domResult.getNode();
    }

}
