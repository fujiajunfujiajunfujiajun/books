package com.sismics.books.rest;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import junit.framework.Assert;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.sismics.books.rest.filter.CookieAuthenticationFilter;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

/**
 * Test the book resource.
 * 
 * @author bgamard
 */
public class TestBookResource extends BaseJerseyTest {
    /**
     * Test the book resource.
     * 
     * @throws Exception 
     */
    @Test
    public void testBookResource() throws Exception {
        // Login book1
        clientUtil.createUser("book1");
        String book1Token = clientUtil.login("book1");
        
        // Add a book
        WebResource bookResource = resource().path("/book");
        bookResource.addFilter(new CookieAuthenticationFilter(book1Token));
        MultivaluedMapImpl postParams = new MultivaluedMapImpl();
        postParams.add("isbn", "9780345376596");
        ClientResponse response = bookResource.put(ClientResponse.class, postParams);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        JSONObject json = response.getEntity(JSONObject.class);
        String book1Id = json.optString("id");
        Assert.assertNotNull(book1Id);
        
        // Get the book
        bookResource = resource().path("/book/" + book1Id);
        bookResource.addFilter(new CookieAuthenticationFilter(book1Token));
        response = bookResource.get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        Assert.assertEquals("Pale Blue Dot", json.getString("title"));
        Assert.assertEquals("A Vision of the Human Future in Space", json.getString("subtitle"));
        Assert.assertEquals("Carl Sagan", json.getString("author"));
        Assert.assertEquals(852073200000l, json.getLong("publish_date"));
        Assert.assertEquals(360, json.getLong("page_count"));
        Assert.assertTrue(json.getString("description").contains("the world beyond Earth"));
        Assert.assertEquals("en", json.getString("language"));
        Assert.assertEquals("0345376595", json.getString("isbn10"));
        Assert.assertEquals("9780345376596", json.getString("isbn13"));

        // Get the book cover
        bookResource = resource().path("/book/" + book1Id + "/cover");
        response = bookResource.get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        InputStream is = response.getEntityInputStream();
        byte[] fileBytes = ByteStreams.toByteArray(is);
        Assert.assertEquals(14406, fileBytes.length);
        
        // List all books
        bookResource = resource().path("/book/list");
        bookResource.addFilter(new CookieAuthenticationFilter(book1Token));
        MultivaluedMapImpl getParams = new MultivaluedMapImpl();
        response = bookResource.queryParams(getParams).get(ClientResponse.class);
        json = response.getEntity(JSONObject.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        JSONArray books = json.getJSONArray("books");
        Assert.assertTrue(books.length() == 1);
        
        // Search the book
        bookResource = resource().path("/book/list");
        bookResource.addFilter(new CookieAuthenticationFilter(book1Token));
        getParams = new MultivaluedMapImpl();
        getParams.add("search", "vision");
        response = bookResource.queryParams(getParams).get(ClientResponse.class);
        json = response.getEntity(JSONObject.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        books = json.getJSONArray("books");
        Assert.assertTrue(books.length() == 1);
        
        // Import a Goodreads CSV
        bookResource = resource().path("/book/import");
        bookResource.addFilter(new CookieAuthenticationFilter(book1Token));
        FormDataMultiPart form = new FormDataMultiPart();
        InputStream goodreadsImport = this.getClass().getResourceAsStream("/import/goodreads.csv");
        FormDataBodyPart fdp = new FormDataBodyPart("file",
                new BufferedInputStream(goodreadsImport),
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
        form.bodyPart(fdp);
        response = bookResource.type(MediaType.MULTIPART_FORM_DATA).put(ClientResponse.class, form);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
    }
}