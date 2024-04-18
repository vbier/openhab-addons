/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.blink.internal.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.blink.internal.dto.BlinkAccount;
import org.openhab.binding.blink.internal.handler.AccountHandler;
import org.openhab.binding.blink.internal.service.AccountService;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AccountVerificationServlet} class provides the servlet for validation of the 2-factor authentication pin
 * provided by blink.
 *
 * The content is loaded from the componentbundle resources (see validation.html)
 *
 * @author Matthias Oesterheld - Initial contribution
 */
@NonNullByDefault
public class AccountVerificationServlet extends HttpServlet {

    public static final long serialVersionUID = 666L;

    private static final String SETTINGS_THINGS = "/settings/things/";
    private final Logger logger = LoggerFactory.getLogger(AccountVerificationServlet.class);
    private final HttpService httpService;
    private final AccountHandler accountHandler;
    private final AccountService blinkService;
    private final String servletUrl;
    private final BundleContext bundleContext;

    public AccountVerificationServlet(HttpService httpService, BundleContext bundleContext,
            AccountHandler accountHandler, AccountService blinkService) {
        this.httpService = httpService;
        this.bundleContext = bundleContext;
        this.accountHandler = accountHandler;
        this.blinkService = blinkService;

        try {
            servletUrl = "/blink/"
                    + URLEncoder.encode(accountHandler.getThing().getUID().getId(), StandardCharsets.UTF_8);
            httpService.registerServlet(servletUrl, this, null, httpService.createDefaultHttpContext());
        } catch (NamespaceException | ServletException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public void dispose() {
        httpService.unregister(servletUrl);
    }

    @Override
    protected void doGet(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response)
            throws IOException {
        if (response == null) {
            logger.warn("Ignoring received request without response.");
            return;
        }
        if (request == null) {
            logger.warn("Ignoring illegal request.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        @Nullable
        BlinkAccount blinkAccount = accountHandler.getBlinkAccount();
        if (request.getParameter("resend") != null && blinkAccount != null) {
            blinkService.login(accountHandler.getConfiguration(), accountHandler.getGeneratedClientId(), true);
        }
        response.addHeader("content-type", "text/html;charset=UTF-8");
        try {
            generateVerificationPage(response.getOutputStream(), false);
        } catch (IOException e) {
            logger.warn("return html failed with uri syntax error", e);
        }
    }

    protected void doPost(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response)
            throws IOException {
        if (response == null) {
            logger.warn("Ignoring received request without response.");
            return;
        }
        if (request == null) {
            logger.warn("Ignoring illegal request.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String pin = IntStream.rangeClosed(1, 6).mapToObj(i -> request.getParameter("digit" + i))
                .filter(Objects::nonNull).collect(Collectors.joining());
        boolean valid = false;
        try {
            valid = blinkService.verifyPin(accountHandler.getBlinkAccount(), pin);
        } catch (IOException e) {
            logger.warn("Error validating blink PIN: {}", e.getMessage());
        }
        if (valid) {
            accountHandler.setOnline();
            response.sendRedirect(SETTINGS_THINGS);
        } else {
            generateVerificationPage(response.getOutputStream(), true);
        }
    }

    void generateVerificationPage(OutputStream outputStream, boolean validationError) throws IOException {
        URL url = this.bundleContext.getBundle().getEntry("org/openhab/binding/blink/internal/servlet/validation.html");
        if (url == null)
            throw new IllegalArgumentException("validation.html not found");
        try (InputStream inputStream = url.openStream()) {
            String verificationPage = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).replace(
                    "{{error}}",
                    validationError ? "<div class=\"error\"><b>Invalid 2 factor verification PIN code.</b><br/>\n"
                            + "    The code is only valid for a 40 minute period. Please try disabling and enabling the blink Account Thing\n"
                            + "    to generate a new PIN code if you think that might be the problem.</div>" : "");
            outputStream.write(verificationPage.getBytes(StandardCharsets.UTF_8));
        }
    }
}
