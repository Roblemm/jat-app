package com.jat.app.service;

public interface JobPageFetcher {

    // Fetches raw page content for public job posts; logged-in pages can still use pastedText instead.
    String fetch(String sourceUrl);
}
