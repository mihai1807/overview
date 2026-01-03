package com.mihai.overview.service;

import com.mihai.overview.request.AuthenticationRequest;
import com.mihai.overview.request.RegisterRequest;
import com.mihai.overview.response.AuthenticationResponse;

public interface AuthenticationService {
    void register(RegisterRequest input) throws Exception;
    AuthenticationResponse login (AuthenticationRequest request);
}
