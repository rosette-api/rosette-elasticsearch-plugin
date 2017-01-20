/*
* Copyright 2017 Basis Technology Corp.
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
package com.rosette.elasticsearch;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.elasticsearch.SpecialPermission;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.index.mapper.StringFieldMapper;
import org.elasticsearch.index.mapper.TypeParsers;

import com.basistech.rosette.api.HttpRosetteAPI;
import com.basistech.rosette.api.HttpRosetteAPIException;
import com.basistech.rosette.apimodel.DocumentRequest;
import com.basistech.rosette.apimodel.LanguageOptions;
import com.basistech.rosette.apimodel.LanguageResponse;

public class ISO639LanguageFieldMapper extends FieldMapper {

    public static final String NAME = "iso639";

    public static class Defaults {
        public static final MappedFieldType FIELD_TYPE =
                new StringFieldMapper.StringFieldType();
        static {
            FIELD_TYPE.setStored(true);
            FIELD_TYPE.setName(NAME);
        }
    }

    public static class Builder extends FieldMapper.Builder<Builder, ISO639LanguageFieldMapper> {

        public Builder(String name) {
            super(name, Defaults.FIELD_TYPE, Defaults.FIELD_TYPE);
            builder = this;
        }

        @Override
        public ISO639LanguageFieldMapper build(BuilderContext context) {
            setupFieldType(context);
            return new ISO639LanguageFieldMapper(name, fieldType, defaultFieldType,
                    context.indexSettings(), multiFieldsBuilder.build(this, context), copyTo);
        }
    }

    public static class TypeParser implements Mapper.TypeParser {
        @Override
        public Mapper.Builder<?, ?> parse(String name, Map<String, Object> node, ParserContext parserContext)
                throws MapperParsingException {
            Builder builder = new Builder(name);
            TypeParsers.parseField(builder, name, node, parserContext);
            return builder;
        }
    }

    protected ISO639LanguageFieldMapper(String simpleName, MappedFieldType fieldType, MappedFieldType defaultFieldType,
                                        Settings indexSettings, MultiFields multiFields, CopyTo copyTo) {
        super(simpleName, fieldType, defaultFieldType, indexSettings, multiFields, copyTo);
    }

    @Override
    protected String contentType() {
        return NAME;
    }

    @Override
    protected void parseCreateField(ParseContext context, List<Field> fields)
            throws IOException {
        final Object value;
        if (context.externalValueSet()) {
            value = context.externalValue();
        } else {
            value = context.parser().textOrNull();
        }
        String textValue = value.toString();
        if (!Strings.isNullOrEmpty(textValue)) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new SpecialPermission());
            }
            // RosApi client binding's Jackson needs elevated privilege
            AccessController.doPrivileged((PrivilegedAction<String>) () -> {
                try {
                    // call /language endpoint and set the result in the field
                    DocumentRequest<LanguageOptions> request = new DocumentRequest.Builder<LanguageOptions>()
                            .content(textValue).build();
                    LanguageResponse response = RosetteApiWrapper.getInstance().getHttpRosetteAPI()
                            .perform(HttpRosetteAPI.LANGUAGE_SERVICE_PATH, request, LanguageResponse.class);
                    fields.add(new StringField(fieldType().name(),
                            // TODO: should we get all detection results and confidence scores?
                            response.getLanguageDetections().get(0).getLanguage().ISO639_3(),
                            fieldType().stored() ? Field.Store.YES : Field.Store.NO));
                } catch (HttpRosetteAPIException e) {
                    // anything bad happens no-op?
                }
                return null;
            });
        }
    }
}
