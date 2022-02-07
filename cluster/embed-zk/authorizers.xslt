<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:pom="http://maven.apache.org/POM/4.0.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:param name="n" required="yes"/>

    <xsl:template match="/authorizers/userGroupProvider/property[@name='Initial User Identity 1']">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
            <xsl:text>CN=nifi</xsl:text>
        </xsl:copy>
        <xsl:for-each select="1 to $n">
            <xsl:text>&#xa;</xsl:text>
            <xsl:element name="property">
                <xsl:attribute name="name">Initial User Identity <xsl:value-of select="position() + 1"/></xsl:attribute>
                <xsl:text>CN=nifi</xsl:text>
                <xsl:value-of select="position()"/>
                <xsl:text>, OU=NIFI</xsl:text>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="/authorizers/accessPolicyProvider/property[@name='Initial Admin Identity']">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
            <xsl:text>CN=nifi</xsl:text>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/authorizers/accessPolicyProvider/property[@name='Node Identity 1']">
        <xsl:for-each select="1 to $n">
            <xsl:element name="property">
                <xsl:attribute name="name">Node Identity <xsl:value-of select="position()"/></xsl:attribute>
                <xsl:text>CN=nifi</xsl:text>
                <xsl:value-of select="position()"/>
                <xsl:text>, OU=NIFI</xsl:text>
            </xsl:element>
            <xsl:text>&#xa;</xsl:text>
        </xsl:for-each>
    </xsl:template>

    <!-- default transform is identity transform; leave everything else intact -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
