package com.jxh.tool.entity;

public class SolrSearch {

    private ResponseHeader responseHeader;
    private Response response;
    private Spellcheck spellcheck;

    public ResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(ResponseHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public Spellcheck getSpellcheck() {
        return spellcheck;
    }

    public void setSpellcheck(Spellcheck spellcheck) {
        this.spellcheck = spellcheck;
    }

}