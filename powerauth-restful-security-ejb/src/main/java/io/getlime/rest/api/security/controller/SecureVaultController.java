/*
 * Copyright 2016 Lime - HighTech Solutions s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.getlime.rest.api.security.controller;

import com.google.common.io.BaseEncoding;
import io.getlime.rest.api.model.base.PowerAuthApiResponse;
import io.getlime.rest.api.model.response.VaultUnlockResponse;
import io.getlime.rest.api.security.exception.PowerAuthAuthenticationException;
import io.getlime.security.powerauth.lib.util.http.PowerAuthHttpBody;
import io.getlime.security.powerauth.lib.util.http.PowerAuthHttpHeader;
import io.getlime.security.soap.client.PowerAuthServiceClient;

import javax.inject.Inject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Controller implementing secure vault related end-points from the
 * PowerAuth Standard API.
 *
 * @author Petr Dvorak, petr@lime-company.eu
 */
@Path("/pa/vault")
public class SecureVaultController {

    private final PowerAuthServiceClient powerAuthClient;

    @Inject
    public SecureVaultController(PowerAuthServiceClient powerAuthClient) {
        this.powerAuthClient = powerAuthClient;
    }

    /**
     * Request the vault unlock key.
     * @param signatureHeader PowerAuth signature HTTP header.
     * @return PowerAuth RESTful response with {@link VaultUnlockResponse} payload.
     * @throws PowerAuthAuthenticationException In case authentication fails.
     * @throws UnsupportedEncodingException In case UTF-8 is not supported.
     */
    @POST
    @Path("unlock")
    public PowerAuthApiResponse<VaultUnlockResponse> unlockVault(
            @HeaderParam(PowerAuthHttpHeader.HEADER_NAME) String signatureHeader)
            throws PowerAuthAuthenticationException, UnsupportedEncodingException {

        Map<String, String> map = PowerAuthHttpHeader.parsePowerAuthSignatureHTTPHeader(signatureHeader);
        String activationId = map.get(PowerAuthHttpHeader.ACTIVATION_ID);
        String applicationId = map.get(PowerAuthHttpHeader.APPLICATION_ID);
        String signature = map.get(PowerAuthHttpHeader.SIGNATURE);
        String signatureType = map.get(PowerAuthHttpHeader.SIGNATURE_TYPE);
        String nonce = map.get(PowerAuthHttpHeader.NONCE);

        String data = PowerAuthHttpBody.getSignatureBaseString("POST", "/pa/vault/unlock", BaseEncoding.base64().decode(nonce), null);

        io.getlime.powerauth.soap.VaultUnlockResponse soapResponse = powerAuthClient.unlockVault(activationId, applicationId, data, signature, signatureType);

        if (!soapResponse.isSignatureValid()) {
            throw new PowerAuthAuthenticationException("USER_NOT_AUTHENTICATED");
        }

        VaultUnlockResponse response = new VaultUnlockResponse();
        response.setActivationId(soapResponse.getActivationId());
        response.setEncryptedVaultEncryptionKey(soapResponse.getEncryptedVaultEncryptionKey());

        return new PowerAuthApiResponse<>("OK", response);
    }

}