/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE
 * or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at license/ESCIDOC.LICENSE.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright 2008 Fachinformationszentrum Karlsruhe Gesellschaft
 * fuer wissenschaftlich-technische Information mbH and Max-Planck-
 * Gesellschaft zur Foerderung der Wissenschaft e.V.  
 * All rights reserved.  Use is subject to license terms.
 */

package de.escidoc.sb.srw;

import java.util.regex.Pattern;

/**
 * Constants for Search.
 * 
 * @author MIH
 */
public class Constants {

    public static final String CHARACTER_ENCODING = "UTF-8";

    public static final Pattern CONTEXT_SET_PATTERN = Pattern.compile("contextSet\\.(.*)");

    public static final Pattern QUALIFIER_PATTERN = Pattern.compile("qualifier\\.(.*)");

    public static final Pattern DOT_PATTERN = Pattern.compile("(.*?)\\.(.*)");

    public static final String GSEARCH_URL = "http://localhost:8080/fedoragsearch/rest";

    public static final String XML_HIT_PATH = "/hits/hit";

}
