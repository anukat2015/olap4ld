/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// Copyright (C) 2007-2010 Julian Hyde
// All Rights Reserved.
// You must accept the terms of that agreement to use this software.
*/
package org.olap4j.driver.olap4ld;

import org.olap4j.impl.Named;
import org.olap4j.metadata.*;

import java.util.Set;

/**
 * Implementation of {@link org.olap4j.metadata.Property}
 * for a member returned on an axis in a cellset
 * from an XML/A provider.
 *
 * @author jhyde
 * @version $Id: XmlaOlap4jCellSetMemberProperty.java 374 2010-12-03 02:29:37Z jhyde $
 * @since Dec 7, 2007
 */
class Olap4ldCellSetMemberProperty implements Property, Named {
    private final String propertyUniqueName;
    final Hierarchy hierarchy;
    final String tag;

    Olap4ldCellSetMemberProperty(
        String propertyUniqueName,
        Hierarchy hierarchy,
        String tag)
    {
        this.propertyUniqueName = propertyUniqueName;
        this.hierarchy = hierarchy;
        this.tag = tag;
    }

    public Datatype getDatatype() {
        return Datatype.STRING;
    }

    public Set<TypeFlag> getType() {
        return TypeFlag.MEMBER_TYPE_FLAG;
    }

    public String getName() {
        return tag;
    }

    public String getUniqueName() {
        return propertyUniqueName;
    }

    public String getCaption() {
        return propertyUniqueName;
    }

    public String getDescription() {
        return "";
    }

    public ContentType getContentType() {
        return ContentType.REGULAR;
    }

    public boolean isVisible() {
        return true;
    }
}

// End XmlaOlap4jCellSetMemberProperty.java
