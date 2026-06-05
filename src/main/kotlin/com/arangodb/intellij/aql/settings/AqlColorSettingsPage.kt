package com.arangodb.intellij.aql.settings

import com.arangodb.intellij.aql.lang.AqlSyntaxColors
import com.arangodb.intellij.aql.lang.AqlSyntaxHighlighter
import com.arangodb.intellij.aql.util.AQL_LANGUAGE_ID
import com.arangodb.intellij.aql.util.Icons
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class AqlColorSettingsPage : ColorSettingsPage {

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Line comment", AqlSyntaxColors.LINE_COMMENT),
            AttributesDescriptor("Block comment", AqlSyntaxColors.BLOCK_COMMENT),
            AttributesDescriptor("Keyword", AqlSyntaxColors.KEYWORD),
            AttributesDescriptor("Function", AqlSyntaxColors.FUNCTION),
            AttributesDescriptor("String", AqlSyntaxColors.STRING),
            AttributesDescriptor("Number", AqlSyntaxColors.NUMBER),
            AttributesDescriptor("Variable", AqlSyntaxColors.VARIABLE),
            AttributesDescriptor("Variable Placeholder", AqlSyntaxColors.VARIABLE_PLACE_HOLDER),
            AttributesDescriptor("Variable Parameter", AqlSyntaxColors.PARAMETER_VARIABLE),
            AttributesDescriptor("Property Lookup", AqlSyntaxColors.PROPERTY_LOOKUP),
            AttributesDescriptor("Operation sign", AqlSyntaxColors.OPERATION_SIGN),
            AttributesDescriptor("Parentheses", AqlSyntaxColors.PARENTHESES),
            AttributesDescriptor("Braces", AqlSyntaxColors.SQUARE_BRACES),
            AttributesDescriptor("Comma", AqlSyntaxColors.COMMA),
            AttributesDescriptor("Dot", AqlSyntaxColors.DOT),
            AttributesDescriptor("EscapeCharacters", AqlSyntaxColors.ESCAPE_CHARACTERS),
            AttributesDescriptor("System Property", AqlSyntaxColors.SYSTEM_PROPERTY)
        )
    }

    override fun getIcon(): Icon = Icons.ICON_ARANGO_SMALL

    override fun getHighlighter(): SyntaxHighlighter = AqlSyntaxHighlighter()

    override fun getDemoText(): String = """
        /* ccc*/

        /**
        * block comment
        */
        // single line comment
        LET isNotify = (FOR doc IN config
        			FILTER doc.id=='notify'
        			RETURN doc.value)
        		LET promotionsBefore = (

        				FOR doc1 IN promotions
        					FILTER doc1.watched ANY == ${'$'}{userId} && doc1.active==true && doc1.weekdays ANY == ${'$'}{weekday}
        					&& doc1.startdate <= ${'$'}{myData} && doc1.finishdate >= ${'$'}{myData}
        					RETURN doc1.id_beonit)
        		LET beaconsBefore = (
        				FOR doc1 IN accesspoints

        				    FILTER POSITION(doc1.users, ${'$'}{userId}) && doc1.startdate <= ${'$'}{myData} && doc1.finishdate >= ${'$'}{myData}
        			RETURN doc1.id_beonit)
        		LET apList = (

        					FOR doc IN accesspoints
        					FILTER doc.accesspoint_id == ${'$'}{minor} && doc.type == "beacon"
        					RETURN doc.id_beonit)
        	    LET apUpdate = (
        	        FOR doc IN accesspoints
        		        FOR ap IN apList
        			        FILTER doc.id_beonit == ap && doc.startdate <= ${'$'}{myData} && doc.finishdate >= @myData
        			            UPDATE
        			                doc WITH {'users':PUSH(doc.users, ${'$'}{userId},true)} IN accesspoints
        			                    RETURN NEW.id_beonit)
        		LET promotionsList = (
        		    FOR doc IN promotions

        		     FOR acp IN doc.accesspoints
        		        FILTER acp IN apList && doc.id_beonit != NULL
        		          RETURN doc.id_beonit)
        		LET promotionsUpdate = (FOR doc IN promotions
        		   FOR pl IN promotionsList
        			FILTER doc.id_beonit==pl && doc.active==true && doc.weekdays ANY == ${'$'}{weekday} && doc.startdate <= ${'$'}{myData} && doc.finishdate >= ${'$'}{myData}
        			UPDATE
        		doc WITH {'watched':PUSH(doc.watched, ${'$'}{userId},true)} IN promotions
        		RETURN NEW.id_beonit)
        		INSERT ${'$'}{req.body} INTO tracking
        		RETURN {'notificationsActive': isNotify[0],'unchangedPromotions': promotionsUpdate ALL IN promotionsBefore, 'unchangedBeacons': apUpdate ALL IN beaconsBefore}
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = AQL_LANGUAGE_ID
}
