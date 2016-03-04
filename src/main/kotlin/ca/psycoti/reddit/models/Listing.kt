package ca.psycoti.reddit.models;

data class Listing(val kind: String, val entries: List<Entry>)

/*
internal object ListingDeserializer: JsonDeserializer<Listing> {
  override fun deserialize(je: JsonElement, type: Type, jdc: JsonDeserializationContext): Listing
  {
    val kind = je.asJsonObject.get("kind")
    val list = je.asJsonObjet.get("data").get("children")

    return Listing(kind, [])
  }
}
*/
