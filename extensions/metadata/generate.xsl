<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    
    <xsl:template match="build">
        <project basedir="." default="jar" name="metadata">
            
            <xsl:comment>This is a generated build file. Do not edit. Change the stylesheet
generate.xsl instead.</xsl:comment>
            
            <property file="local.properties"/>
            <property file="build.properties"/>
            
            <xsl:apply-templates select="backends"/>
            
            <target name="compile">
                <echo message="---------------------------"/>
                <echo message="Compiling backends"/>
                <echo message="---------------------------"/>
                <iterate target="compile"/>
            </target>
            
            <target name="compile-tests">
                <echo message="---------------------------"/>
                <echo message="Compiling backends tests"/>
                <echo message="---------------------------"/>
                <iterate target="compile-tests"/>
            </target>
            
            <target name="jar">
                <echo message="---------------------------"/>
                <echo message="Creating jars for backend"/>
                <echo message="---------------------------"/>
                <iterate target="jar"/>
            </target>
            
            <target name="clean">
                <echo message="-------------------------------------"/>
                <echo message="Cleaning backends ..."/>
                <echo message="-------------------------------------"/>
                <iterate target="clean"/>
                <delete file="build.xml" failonerror="false"/>
            </target>
            
            <target name="all-clean">
                <iterate target="all-clean"/>
            </target>
            
            <target name="test">
                <echo message="------------------------------------------"/>
                <echo message="Running tests on backends"/>
                <echo message="------------------------------------------"/>
                <iterate target="test"/>
            </target>
            
        </project>
    </xsl:template>
    
    <xsl:template match="backends">
        <xsl:for-each select="backend">
            <condition property="include.metadata.{@name}.config">
                <istrue value="${{include.metadata.{@name}}}"/>
            </condition>
        </xsl:for-each>
        <xsl:apply-templates select="backend"/>
        <macrodef name="iterate">
            <attribute name="target"/>
            <sequential>
                <xsl:for-each select="backend">
                    <antcall target="{@name}">
                        <param name="target" value="@{{target}}"/>
                    </antcall>
                </xsl:for-each>
            </sequential>
        </macrodef>
    </xsl:template>
    
    <xsl:template match="backend">
        <target name="{@name}" if="include.metadata.{@name}.config">
            <ant antfile="{@antfile}" dir="{@dir}" target="${{target}}">
                <property name="module" value="{@name}"/>
            </ant>
        </target>
    </xsl:template>
    
</xsl:stylesheet>