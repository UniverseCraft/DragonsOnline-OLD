package mc.dragons.core.storage.impl;

import java.util.Map.Entry;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import mc.dragons.core.storage.Identifier;
import mc.dragons.core.storage.StorageAccess;

/**
 * Implementation of {@link mc.dragons.core.storage.StorageAccess}
 * for MongoDB.
 * 
 * @author Rick
 *
 */
public class MongoStorageAccess implements StorageAccess {
	private Identifier identifier;
	private Document document;
	private MongoCollection<Document> collection;
	
	public MongoStorageAccess(Identifier identifier, Document document, MongoCollection<Document> collection) {
		this.identifier = identifier;
		this.document = document.append("type", identifier.getType().toString()).append("_id", identifier.getUUID());
		this.collection = collection;
	}

	public void set(String key, Object value) {
		if(key.equals("type") || key.equals("_id")) {
			throw new IllegalArgumentException("Cannot modify type or UUID of storage access once instantiated");
		}
		document.append(key, value);
		update(new Document(key, value));
	}
	
	public void update(Document document) {
		this.document.putAll(document);
		collection.updateOne(identifier.getDocument(), new Document("$set", document));
	}

	public Object get(String key) {
		return document.get(key);
	}
	
	public Set<Entry<String, Object>> getAll() {
		return document.entrySet();
	}
	
	public Document getDocument() {
		return document;
	}
	
	public Identifier getIdentifier() {
		return identifier;
	}
}
