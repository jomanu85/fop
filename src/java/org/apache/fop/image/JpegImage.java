/*
 * Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 
package org.apache.fop.image;

// Java
import java.io.ByteArrayOutputStream;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;

// FOP
import org.apache.commons.io.IOUtils;
import org.apache.fop.util.CMYKColorSpace;

/**
 * FopImage object for JPEG images, Using Java native classes.
 * @author Eric Dalquist
 * @see AbstractFopImage
 * @see FopImage
 */
public class JpegImage extends AbstractFopImage {
    private ICC_Profile iccProfile = null;
    private boolean foundICCProfile = false;

    /**
     * Create a jpeg image with the info.
     *
     * @param imgInfo the image info for this jpeg
     */
    public JpegImage(FopImage.ImageInfo imgInfo) {
        super(imgInfo);
    }

    /**
     * Load the original jpeg data.
     * This loads the original jpeg data and reads the color space,
     * and icc profile if any.
     *
     * @return true if loaded false for any error
     */
    protected boolean loadOriginalData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayOutputStream iccStream = new ByteArrayOutputStream();
        int index = 0;
        boolean cont = true;

        try {
            byte[] readBuf = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(readBuf)) != -1) {
                baos.write(readBuf, 0, bytesRead);
            }
        } catch (java.io.IOException ex) {
            log.error("Error while loading image (Jpeg): " + ex.getMessage(), ex);
            return false;
        } finally {
            IOUtils.closeQuietly(inputStream);
            inputStream = null;
        }

        this.raw = baos.toByteArray();
        this.bitsPerPixel = 8;
        this.isTransparent = false;

        //Check for SOI (Start of image) marker (FFD8)
        if (this.raw.length > (index + 2)
                && uByte(this.raw[index]) == 255 /*0xFF*/
                && uByte(this.raw[index + 1]) == 216 /*0xD8*/) {
            index += 2;

            while (index < this.raw.length && cont) {
                //check to be sure this is the begining of a header
                if (this.raw.length > (index + 2)
                        && uByte(this.raw[index]) == 255 /*0xFF*/) {

                    //192 or 194 are the header bytes that contain
                    // the jpeg width height and color depth.
                    if (uByte(this.raw[index + 1]) == 192 /*0xC0*/
                            || uByte(this.raw[index + 1]) == 194 /*0xC2*/) {

                        this.height = calcBytes(this.raw[index + 5],
                                                  this.raw[index + 6]);
                        this.width = calcBytes(this.raw[index + 7],
                                                 this.raw[index + 8]);

                        if (this.raw[index + 9] == 1) {
                            this.colorSpace = ColorSpace.getInstance(
                              ColorSpace.CS_GRAY);
                        } else if (this.raw[index + 9] == 3) {
                            this.colorSpace = ColorSpace.getInstance(
                              ColorSpace.CS_LINEAR_RGB);
                        } else if (this.raw[index + 9] == 4) {
                            // howto create CMYK color space
                            /*
                            this.colorSpace = ColorSpace.getInstance(
                              ColorSpace.CS_CIEXYZ);
                            */
                            this.colorSpace = CMYKColorSpace.getInstance();
                        } else {
                            log.error("Unknown ColorSpace for image: "
                                                   + "");
                            return false;
                        }

                        if (foundICCProfile) {
                            cont = false;
                            break;
                        }
                        index += calcBytes(this.raw[index + 2],
                                           this.raw[index + 3]) + 2;

                    } else if (uByte(this.raw[index + 1]) == 226 /*0xE2*/
                                   && this.raw.length > (index + 60)) {
                        // Check if ICC profile
                        byte[] iccString = new byte[11];
                        System.arraycopy(this.raw, index + 4,
                                         iccString, 0, 11);

                        if ("ICC_PROFILE".equals(new String(iccString))) {
                            int chunkSize = calcBytes(
                                              this.raw[index + 2],
                                              this.raw[index + 3]) + 2;

                            iccStream.write(this.raw,
                                            index + 18, chunkSize - 18);

                        }

                        index += calcBytes(this.raw[index + 2],
                                           this.raw[index + 3]) + 2;
                    } else {
                        index += calcBytes(this.raw[index + 2],
                                           this.raw[index + 3]) + 2;
                    }

                } else {
                    cont = false;
                }
            }
        } else {
            log.error("Error while loading "
                         + "JpegImage - Invalid JPEG Header.");
            return false;
        }
        if (iccStream.size() > 0) {
            byte[] align = new byte[((iccStream.size()) % 8) + 8];
            try {
                iccStream.write(align);
            } catch (Exception ex) {
                log.error("Error while aligning ICC stream: " + ex.getMessage(), ex);
                return false;
            }
            try {
                iccProfile = ICC_Profile.getInstance(iccStream.toByteArray());
            } catch (Exception e) {
                log.error("Invalid ICC profile: " + e, e);
                return false;
            }
        } else if (this.colorSpace == null) {
            log.error("ColorSpace not specified for JPEG image");
            return false;
        }
        return true;
    }

    /**
     * Get the ICC profile for this Jpeg image.
     *
     * @return the icc profile or null if not found
     */
    public ICC_Profile getICCProfile() {
        return iccProfile;
    }

    private int calcBytes(byte bOne, byte bTwo) {
        return (uByte(bOne) * 256) + uByte(bTwo);
    }

    private int uByte(byte bIn) {
        if (bIn < 0) {
            return 256 + bIn;
        } else {
            return bIn;
        }
    }
}

