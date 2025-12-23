package com.mihai.overview.service;

import com.mihai.overview.request.RegisterRequest;

public interface AuthenticationService {
    void register(RegisterRequest input) throws Exception;
}
