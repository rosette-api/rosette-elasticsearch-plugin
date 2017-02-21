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

package com.rosette.elasticsearch.demoingest;

import com.basistech.internal.util.ISO15924Utils;
import com.basistech.rosette.api.HttpRosetteAPI;
import com.basistech.rosette.api.common.AbstractRosetteAPI;
import com.basistech.rosette.apimodel.CategoriesOptions;
import com.basistech.rosette.apimodel.CategoriesResponse;
import com.basistech.rosette.apimodel.DocumentRequest;
import com.basistech.rosette.apimodel.EntitiesOptions;
import com.basistech.rosette.apimodel.LanguageOptions;
import com.basistech.rosette.apimodel.LanguageResponse;
import com.basistech.rosette.apimodel.NameTranslationRequest;
import com.basistech.rosette.apimodel.NameTranslationResponse;
import com.basistech.rosette.apimodel.SentimentOptions;
import com.basistech.rosette.dm.AnnotatedText;
import com.basistech.rosette.dm.Entity;
import com.basistech.rosette.dm.Mention;
import com.basistech.util.ISO15924;
import com.basistech.util.LanguageCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs documents through the Rosette API and indexes them in an Elasticsearch instance for demo purposes.
 */
public class DemoIngestCLI {
    private TransportClient client;
    private HttpRosetteAPI rosetteApi;
    private String indexName;
    private String typeName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    DemoIngestCLI(String transportURL, String clusterName, String indexName, String typeName, String apikey) throws IOException {
        this.indexName = indexName;
        this.typeName = typeName;

        //Connect to the Elasticsearch instance
        this.client = connectRemoteES(transportURL, clusterName);

        //Copy the mapping from the classpath
        String mapping = copyToStringFromClasspath("/mapping/demo-mapping.json");

        //Create the index if it doesn't already exist
        if (!client.admin().indices().prepareExists(indexName).get().isExists()) {
            client.admin().indices().prepareCreate(indexName).addMapping(typeName, mapping).get();
        }

        //Get a hold of the Rosette API
        this.rosetteApi = new HttpRosetteAPI.Builder().key(apikey).build();
    }

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("transportURL").hasArg().isRequired().withDescription("transport URL of a running Elasticsearch instance").create("t"));
        options.addOption(OptionBuilder.withLongOpt("inputPath").hasArg().isRequired().withDescription("filepath to a directory tree containing documents to ingest").create("d"));
        options.addOption(OptionBuilder.withLongOpt("apiKey").hasArg().isRequired().withDescription("valid Rosette API key to use").create("k"));
        options.addOption("indexName", true, "name of the index, defaults to 'demo'");
        options.addOption("clusterName", true, "name of the Elasticsearch cluster to connect to");
        options.addOption("typeName", true, "name of the document type, defaults to 'demo'");

        CommandLineParser parser = new BasicParser();
        CommandLine cmdArgs = parser.parse(options, args);

        DemoIngestCLI demo = new DemoIngestCLI(cmdArgs.getOptionValue("transportURL"),
                cmdArgs.getOptionValue("clusterName", "elasticsearch"),
                cmdArgs.getOptionValue("indexName", "demo"),
                cmdArgs.getOptionValue("typeName", "demo"),
                cmdArgs.getOptionValue("apiKey"));

        Path directory = Paths.get(cmdArgs.getOptionValue("inputPath"));

        //Walks the input directory tree and process every file found. Expects them to be the output from the RSS Harvester
        //TODO:
        //  - This could be multi-threaded
        //  - Improved error handling
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                demo.processFile(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Objects.requireNonNull(dir);
                Objects.requireNonNull(attrs);
                System.out.println("Adding documents from:" + dir.toString());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    //Parse, enrich, and index the given file
    void processFile(Path file) {
        try {
            //Expects the file to be in the RSS Harvester output format
            JsonNode json = objectMapper.readTree(file.toFile());

            RosetteResult rosetteResult = processDocument(json.get("text").asText());

            indexDocument(file.getFileName().toString(), rosetteResult, json.findValue("title").asText(), json.findValue("url").asText(), json.findValue("$date").asText());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    //Enrich the document using RosAPI
    RosetteResult processDocument(String content) throws IOException {
        //This simple object holds all the results
        RosetteResult result = new RosetteResult();
        result.text = content;

        //RLI
        DocumentRequest<LanguageOptions> langrequest = new DocumentRequest.Builder<LanguageOptions>().content(result.text).build();
        LanguageResponse langresponse = rosetteApi.perform(AbstractRosetteAPI.LANGUAGE_SERVICE_PATH, langrequest, LanguageResponse.class);
        LanguageCode docLang = LanguageCode.normalizeNonStandard(langresponse.getLanguageDetections().get(0).getLanguage());
        result.lang = docLang.ISO639_3();

        //Currently skipping further processing for Russian and Chinese documents
        if (docLang == LanguageCode.RUSSIAN || docLang == LanguageCode.CHINESE) {
            return result;
        }

        //TCAT
        if (docLang == LanguageCode.ENGLISH) {
            DocumentRequest<CategoriesOptions> catrequest = new DocumentRequest.Builder<CategoriesOptions>().content(result.text).build();
            CategoriesResponse catresponse = rosetteApi.perform(HttpRosetteAPI.CATEGORIES_SERVICE_PATH, catrequest, CategoriesResponse.class);
            result.catLabel = catresponse.getCategories().get(0).getLabel();
        }

        //Need to use the ADM for entities so we get offsets
        AnnotatedText adm;

        //SENTIMENT
        if (docLang == LanguageCode.ENGLISH || docLang == LanguageCode.SPANISH || docLang == LanguageCode.JAPANESE) {
            DocumentRequest<SentimentOptions> sentrequest = new DocumentRequest.Builder<SentimentOptions>().content(result.text).build();
            adm = rosetteApi.perform(AbstractRosetteAPI.SENTIMENT_SERVICE_PATH, sentrequest);
            result.sentLabel = adm.getSentimentResults().get(0).getLabel();
        } else {
            //REX
            DocumentRequest<EntitiesOptions> entityrequest = new DocumentRequest.Builder<EntitiesOptions>().content(result.text).build();
            adm = rosetteApi.perform(AbstractRosetteAPI.ENTITIES_SERVICE_PATH, entityrequest);
        }

        //Gather the desired info about the entities and get translations if appropriate
        for (Entity entity : adm.getEntities()) {
            EntityResult er = createEntity(entity);
            result.entities.add(er);
        }

        return result;
    }

    //Calls RNT if necessary for a translation
    private EntityResult createEntity(Entity entity) {
        List<Mention> mentions = entity.getMentions();
        List<EntityOffset> offsets = new ArrayList<>(mentions.size());
        mentions.forEach(m -> offsets.add(new EntityOffset(m.getStartOffset(), m.getEndOffset())));
        String headMention = mentions.get(entity.getHeadMentionIndex()).getNormalized();
        String type = entity.getType();

        EntityResult er = new EntityResult(headMention, type, entity.getEntityId(), mentions.size(), offsets);
        //RNT
        if (ISO15924Utils.scriptForString(headMention) != ISO15924.Latn
                && (type.equalsIgnoreCase("PERSON") || type.equalsIgnoreCase("LOCATION") || type.equalsIgnoreCase("ORGANIZATION"))) {
            NameTranslationRequest rntrequest = new NameTranslationRequest.Builder(headMention, LanguageCode.ENGLISH).entityType(type).build();
            NameTranslationResponse rntresponse = rosetteApi.perform(AbstractRosetteAPI.NAME_TRANSLATION_SERVICE_PATH, rntrequest, NameTranslationResponse.class);
            er.translation = rntresponse.getTranslation();
        }

        if (entity.getSentiment() != null) {
            er.sentiment = entity.getSentiment().get(0).getLabel();
        }
        return er;
    }

    //Index the enriched document in Elasticsearch. The filename is used as the doc id
    IndexResponse indexDocument(String filename, RosetteResult rosetteResult, String title, String url, String date) throws IOException {
        //Build the ES doc
        XContentBuilder builder = XContentFactory.jsonBuilder().startObject();

        rosetteResult.toXContent(builder);

        builder.field("title", title);
        builder.field("url", url);
        builder.field("date", date);

        //Send it
        return client.prepareIndex(indexName, typeName)
                     .setId(filename) //use the name of the file as the id to prevent duplicate entries
                     .setSource(builder.endObject())
                     .setContentType(XContentType.JSON)
                     .get();
    }

    private static TransportClient connectRemoteES(String urlString, String clusterName) throws IOException {
        URL url = new URL(urlString);
        InetSocketTransportAddress address = new InetSocketTransportAddress(InetAddress.getByName(url.getHost()), url.getPort());
        Settings settings = Settings.builder().put("cluster.name", clusterName).build();

        TransportClient c = new PreBuiltTransportClient(settings).addTransportAddress(address);

        return c;
    }

    private static String copyToStringFromClasspath(String path) throws IOException {
        InputStream is = Streams.class.getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException("Resource [" + path + "] not found in classpath");
        }
        return Streams.copyToString(new InputStreamReader(is, Charsets.UTF_8));
    }

    //The following are simple objects to store results and handle serialization

    class RosetteResult {

        String text;
        String sentLabel;
        String catLabel;
        String lang;
        List<EntityResult> entities;

        RosetteResult() {
            entities = new ArrayList<>();
        }

        XContentBuilder toXContent(XContentBuilder builder) throws IOException {
            builder.field("text", this.text);

            builder.field("language", this.lang);

            if (this.sentLabel != null) {
                builder.field("sentiment", this.sentLabel);
            }

            if (this.catLabel != null) {
                builder.field("category", this.catLabel);
            }

            builder.startArray("entity");
            for (EntityResult entity : this.entities) {
                builder.startObject();
                entity.toXContent(builder);
                builder.endObject();
            }
            builder.endArray();

            return builder;
        }

    }

    class EntityResult {
        String type;
        String normalized;
        String entityId;
        String sentiment;
        String translation;
        int count;
        List<EntityOffset> offsets;

        EntityResult(String normalized, String type, String entityId, int count, List<EntityOffset> offsets) {
            this.type = type;
            this.normalized = normalized;
            this.entityId = entityId;
            this.count = count;
            this.offsets = offsets;
        }

        XContentBuilder toXContent(XContentBuilder builder) throws IOException {
            builder.field("mention", this.normalized)
                    .field("qid", this.entityId)
                    .field("type", this.type)
                    .field("count", this.count);

            builder.startArray("offsets");
            for (EntityOffset offset : this.offsets) {
                builder.startObject();
                offset.toXContent(builder);
                builder.endObject();
            }
            builder.endArray();

            if (this.sentiment != null) {
                builder.field("sentiment", this.sentiment);
            }
            if (this.translation != null) {
                builder.field("mention_eng", this.translation);
            }

            return builder;
        }
    }

    class EntityOffset {
        int start;
        int end;

        EntityOffset(int start, int end) {
            this.start = start;
            this.end = end;
        }

        XContentBuilder toXContent(XContentBuilder builder) throws IOException {
            builder.field("start", this.start)
                    .field("end", this.end);

            return builder;
        }
    }
}
