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
package org.openhab.binding.blink.internal.dto;

import java.util.List;

/**
 * The {@link BlinkHomescreen} class is the DTO for the homescreen api call, wrapping networks and cameras with their
 * states.
 *
 * @author Matthias Oesterheld - Initial contribution
 */
public class BlinkHomescreen {

    public List<BlinkNetwork> networks;
    public List<BlinkCamera> cameras;
    public List<BlinkCamera> doorbells;
    public List<BlinkCamera> owls;
}
