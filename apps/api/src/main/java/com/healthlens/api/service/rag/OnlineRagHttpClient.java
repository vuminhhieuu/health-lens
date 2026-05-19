package com.healthlens.api.service.rag;

import java.net.URI;

public interface OnlineRagHttpClient {
    String fetch(URI uri);
}
