package com.adobe.acs.commons.requestwrappers;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {
    private static final Logger log = LoggerFactory.getLogger(MultiReadHttpServletRequest.class);

    private ByteArrayOutputStream cachedBytes;

    public MultiReadHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);

        // Immediately cache the input stream to prevent other super methods for accessing the
        // super InputStream before it can be cached via a call to getInputStream()
        cacheInputStream();
    }

    @Override
    public final ServletInputStream getInputStream() throws IOException {
        return new CachedServletInputStream();
    }

    @Override
    public final BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    private void cacheInputStream() throws IOException {
        // Cache the InputStream in order to read it multiple times.
        cachedBytes = new ByteArrayOutputStream();
        IOUtils.copy(super.getInputStream(), cachedBytes);
    }

    /**
     *  An InputStream which reads the cached request body.
     */
    public class CachedServletInputStream extends ServletInputStream {
        private ByteArrayInputStream input;

        public CachedServletInputStream() {
            // Create a new input stream from the cached request body
            input = new ByteArrayInputStream(cachedBytes.toByteArray());
        }

        @Override
        public final int read() throws IOException {
            return input.read();
        }
    }
}