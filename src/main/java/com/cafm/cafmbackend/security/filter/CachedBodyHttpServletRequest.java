package com.cafm.cafmbackend.security.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Custom HttpServletRequest wrapper that caches the request body for multiple reads.
 * 
 * Purpose: Allows request body to be read multiple times in the filter chain
 * Pattern: Decorator pattern for HttpServletRequest
 * Java 23: Modern I/O handling with try-with-resources
 * Architecture: Cross-cutting concern for request processing
 * Standards: Servlet API best practices for request wrapping
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    
    private static final Logger logger = LoggerFactory.getLogger(CachedBodyHttpServletRequest.class);
    
    private final byte[] cachedBody;
    
    /**
     * Create a new CachedBodyHttpServletRequest.
     * Reads and caches the request body immediately.
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        
        // Read the request body and cache it
        InputStream requestInputStream = request.getInputStream();
        this.cachedBody = StreamUtils.copyToByteArray(requestInputStream);
        
        logger.debug("Cached request body: {} bytes", cachedBody.length);
    }
    
    /**
     * Get the cached request body as a byte array.
     */
    public byte[] getCachedBody() {
        return cachedBody;
    }
    
    /**
     * Get the cached request body as a String.
     */
    public String getCachedBodyAsString() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }
    
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedBodyServletInputStream(cachedBody);
    }
    
    @Override
    public BufferedReader getReader() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream, getCharacterEncoding()));
    }
    
    /**
     * Custom ServletInputStream that reads from the cached body.
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        
        private final ByteArrayInputStream inputStream;
        
        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }
        
        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            return inputStream.read(b);
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return inputStream.read(b, off, len);
        }
        
        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }
        
        @Override
        public boolean isReady() {
            return true;
        }
        
        @Override
        public void setReadListener(ReadListener listener) {
            // Not implemented for cached body
            throw new UnsupportedOperationException("setReadListener not supported");
        }
        
        @Override
        public int available() throws IOException {
            return inputStream.available();
        }
        
        @Override
        public void close() throws IOException {
            inputStream.close();
        }
        
        @Override
        public void mark(int readlimit) {
            inputStream.mark(readlimit);
        }
        
        @Override
        public void reset() throws IOException {
            inputStream.reset();
        }
        
        @Override
        public boolean markSupported() {
            return inputStream.markSupported();
        }
    }
}