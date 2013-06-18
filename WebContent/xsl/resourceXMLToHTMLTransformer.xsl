<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml"
	xmlns:atom="http://www.w3.org/2005/Atom" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:apiscol="http://www.crdp.ac-versailles.fr/2012/apiscol"
	exclude-result-prefixes="#default">
	<xsl:output method="html" omit-xml-declaration="yes"
		encoding="UTF-8" indent="yes" />
	<xsl:template match="/">
		<html>
			<head>
				<meta charset="utf-8">
				</meta>


				<style>
					/* ------------------
					styling for the tables
					------------------ */

					div.apiscol-list table#hor-minimalist-a thead
					tr th img {
					vertical-align: bottom;
					display: inline-block;
					margin-right: 1em;
					}

					#hor-minimalist-a {
					font-family: "Lucida Sans
					Unicode", "Lucida Grande", Sans-Serif;
					font-size: 12px;
					background:
					#fff;
					margin: 45px;
					border-collapse: collapse;
					text-align: left;
					vertical-align: top;
					width: 1000px;
					table-layout: fixed;
					}
					#hor-minimalist-a tbody tr.bottom td details div {
					padding-bottom:0.5em;
					}
					#hor-minimalist-a tr,#hor-minimalist-a td {
					vertical-align: top;
					}

					#hor-minimalist-a tbody tr td,
					#hor-minimalist-a tbody tr div{

					}
					#hor-minimalist-a tbody tr div{
					margin: 0;
					padding: 0;
					}

					#hor-minimalist-a tbody tr.bottom {
					border-bottom: thin solid
					#6678b1;
					padding-bottom: 0.5em;
					}

					#hor-minimalist-a th {
					font-size:
					14px;
					font-weight: normal;
					color:
					#039;
					padding: 10px 8px;
					border-bottom: 2px solid #6678b1;
					}

					#hor-minimalist-a td {
					color:
					#669;
					padding: 9px 8px 0px 8px;
					}

					#hor-minimalist-a tbody tr:hover td
					{
					color: #009;
					}

				</style>
				<xsl:element name="meta" namespace="">
					<xsl:attribute name="name">
						<xsl:value-of select="'description'"></xsl:value-of>
						</xsl:attribute>
					<xsl:attribute name="content">
						<xsl:value-of select="'Séquences pédagogiques'"></xsl:value-of>
						</xsl:attribute>
				</xsl:element>
				<meta name="viewport" content="width=device-width"></meta>

			</head>
			<body>
				<div class="apiscol-list">
					<table id="hor-minimalist-a">
						<xsl:apply-templates select="*[local-name()='entry']"></xsl:apply-templates>
					</table>
				</div>

			</body>



		</html>

	</xsl:template>

	<xsl:template match="*[local-name()='entry']">
		<tr>
			<td colspan="12">
				<strong>
					<xsl:apply-templates select="*[local-name()='content'][@type='text/html']"></xsl:apply-templates>
					<xsl:apply-templates
						select="*[local-name()='content'][@type='application/xml']"></xsl:apply-templates>
				</strong>
			</td>


		</tr>
		<tr>
			<td>
				<xsl:element name="a">
					<xsl:attribute name="href">
			<xsl:value-of
						select="*[local-name()='link'][@type='text/html'][@rel='self']/@href"></xsl:value-of>
			</xsl:attribute>
					HTML
				</xsl:element>
			</td>
			<td>
				<xsl:element name="a">
					<xsl:attribute name="href">
			<xsl:value-of
						select="*[local-name()='link'][@type='application/atom+xml'][@rel='self']/@href"></xsl:value-of>
			</xsl:attribute>
					ATOM
				</xsl:element>
			</td>
			<td>
				<xsl:element name="a">
					<xsl:attribute name="href">
			<xsl:value-of
						select="*[local-name()='link'][@type='application/atom+xml'][@rel='describedby']/@href"></xsl:value-of>
			</xsl:attribute>
					META
				</xsl:element>
			</td>
			<td>
				<xsl:element name="a">
					<xsl:attribute name="href">
			<xsl:value-of
						select="*[local-name()='link'][@type='text/html'][@rel='preview']/@href"></xsl:value-of>
			</xsl:attribute>
					PREVIEW
				</xsl:element>
			</td>
			<td colspan="7">
				<xsl:value-of select="*[local-name()='id']"></xsl:value-of>
			</td>

		</tr>
		<tr class="bottom">
			<td colspan="12">

				<xsl:apply-templates
					select="*[local-name()='content'][@type='application/xml']/*[local-name()='files']"></xsl:apply-templates>

			</td>
		</tr>
	</xsl:template>
	<xsl:template
		match="*[local-name()='content'][@type='application/xml']/*[local-name()='files']">
		<details>
			<summary>Fichiers</summary>
			<div>
				<xsl:apply-templates select="*[local-name()='file']"></xsl:apply-templates>
			</div>
		</details>
	</xsl:template>
	<xsl:template match="*[local-name()='file']">
		<span>
			<xsl:element name="a">
				<xsl:attribute name="href">
			<xsl:value-of select="link/@href"></xsl:value-of>
			</xsl:attribute>
				<xsl:value-of select="title"></xsl:value-of>
			</xsl:element>
		</span>
		&#0160;

	</xsl:template>
	<xsl:template name="pagination">
		<xsl:param name="step"></xsl:param>
		<xsl:param name="length"></xsl:param>
		<xsl:element name="a">
			<xsl:attribute name="href">
		<xsl:value-of select="concat('?start=', $step*10, '&amp;rows=10')"></xsl:value-of>
		</xsl:attribute>
			<xsl:value-of select="$step+1"></xsl:value-of>
		</xsl:element>
		&#0160;
		<xsl:choose>
			<xsl:when test="($step+1)*10>$length"></xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="pagination">
					<xsl:with-param name="length" select="$length">
					</xsl:with-param>
					<xsl:with-param name="step" select="$step+1">
					</xsl:with-param>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="page_number">
		<xsl:param name="total"></xsl:param>
		<xsl:param name="start"></xsl:param>
		<xsl:param name="rows"></xsl:param>
		<xsl:variable name="one" select="1"></xsl:variable>
		<xsl:variable name="rowsminusone" select="($rows)-($one)"></xsl:variable>
		Résultats
		<xsl:value-of select="$start"></xsl:value-of>
		-
		<xsl:value-of select="$start+$rowsminusone"></xsl:value-of>
		/
		<xsl:value-of select="$total"></xsl:value-of>
	</xsl:template>
	<xsl:template match="*[local-name()='content'][@type='text/html']">
		<xsl:element name="a">
			<xsl:attribute name="href">
		<xsl:value-of select="*[local-name()='a']/@href"></xsl:value-of>
		</xsl:attribute>
			<xsl:value-of select="*[local-name()='a']/@href"></xsl:value-of>
		</xsl:element>
	</xsl:template>
	<xsl:template match="*[local-name()='content'][@type='application/xml']">
		<xsl:element name="a">
			<xsl:attribute name="href"><xsl:value-of
				select="*[local-name()='archive']/@src"></xsl:value-of></xsl:attribute>
			<xsl:value-of select="*[local-name()='files']/@main"></xsl:value-of>
		</xsl:element>

	</xsl:template>

</xsl:stylesheet>
