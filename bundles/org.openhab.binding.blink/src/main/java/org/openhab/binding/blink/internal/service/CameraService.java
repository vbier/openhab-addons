/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 * <p>
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.blink.internal.service;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.blink.internal.config.CameraConfiguration;
import org.openhab.binding.blink.internal.dto.BlinkAccount;
import org.openhab.binding.blink.internal.dto.BlinkCommand;

import com.google.gson.Gson;

/**
 * The {@link CameraService} class handles all communication with camera related blink apis.
 *
 * @author Matthias Oesterheld - Initial contribution
 */
@NonNullByDefault
public class CameraService extends BaseBlinkApiService {
    public CameraService(HttpClient httpClient, Gson gson) {
        super(httpClient, gson);
    }

    /**
     * Call motion detection endpoints do enable/disable motion detection for the given camera.
     *
     * @param account blink account
     * @param camera blink camera
     * @param enable enable/disable
     * @return blink async command ID
     */
    public Long motionDetection(@Nullable BlinkAccount account, @Nullable CameraConfiguration camera, boolean enable)
            throws IOException {
        if (account == null || account.account == null || camera == null)
            throw new IllegalArgumentException("Cannot call motion detection api without account or camera");
        String action = (enable) ? "/enable" : "/disable";
        String uri = "/network/" + camera.networkId + "/camera/" + camera.cameraId + action;
        BlinkCommand cmd = apiRequest(account.account.tier, uri, HttpMethod.POST, account.auth.token, null,
                BlinkCommand.class);
        return cmd.id;
    }

    /**
     * Call thumbnail endpoint to create a thumbnail for the given camera.
     *
     * @param account blink account
     * @param camera blink camera
     * @return blink async command ID
     */
    public Long createThumbnail(@Nullable BlinkAccount account, @Nullable CameraConfiguration camera)
            throws IOException {
        if (account == null || account.account == null || camera == null)
            throw new IllegalArgumentException("Cannot call thumbnail api without account or camera");
        String uri = "/network/" + camera.networkId + "/camera/" + camera.cameraId + "/thumbnail";
        BlinkCommand cmd = apiRequest(account.account.tier, uri, HttpMethod.POST, account.auth.token, null,
                BlinkCommand.class);
        return cmd.id;
    }

    public byte[] getThumbnail(@Nullable BlinkAccount account, String imagePath) throws IOException {
        if (account == null || account.account == null)
            throw new IllegalArgumentException("Cannot call get thumbnail api without account");
        if (!imagePath.endsWith(".jpg"))
            imagePath += ".jpg";
        return rawRequest(account.account.tier, imagePath, HttpMethod.GET, account.auth.token, null);
    }
}
