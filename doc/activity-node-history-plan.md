Create API and its implementation that allows recording on the home node of the names of the visited nodes and looking
for them.

Create /people/visited API endpoint. It should provide POST, DELETE and GET operations. All operations are admin only.

POST operation should accept an object in the request body containing the visited node name. It should create a favor of
type FavorType.VISITED. Its `value` is 10f and `decayHours` should be 10 days. Repeated visits to the same node should
create multiple VISITED favors. In addition to that, there should be visitCount field in Contact. It should be
incremented by 1 when a new visit is recorded. The ContactOperations method that updates visitCount should create the
Contact record when it does not exist, as the existing contact creation path lets DB triggers create profile subscription
and avatar lookup records automatically.

Add favor type persistence. The favors table should have a favor_type column populated from FavorType, and all existing
favor creation, lookup, expiration and deletion routines should account for it. Existing favors from older migrations may
be assigned a type or otherwise handled so distance calculation keeps its current behavior.

Once a day, recompute visitCount for contacts that have visitCount > 0 from the number of non-expired VISITED favors.
The expiration routine should not rely on per-expired-favor decrementing to keep visitCount correct.

DELETE operation should remove all favors of type FavorType.VISITED for the particular node and set visitCount to 0.

FavorOperations.updateDistance() should consider visitCount > 0 as distance -= .25f.

GET operation should return the list visited nodes, filtered by `query` parameter and limited by `limit` parameter, just
like GET /people/contacts does. Move the routines used by ContactsController for this purpose to reuse them. Only return
nodes that have visitCount > 0. Sort by Contact.distance, like /people/contacts. Use SearchNodeInfo for the results and
fill full name, avatar and distance only from the existing Contact data; do not fetch missing details while serving this
endpoint.

Do not forget to create DB migration scripts and additional indexes if needed.

On the client side (../moera-client-react/), when the search suggestions are opened, query /people/visited with limit=4
and put the results before other suggested node names returned by the search engine, but after search history. Avoid
duplicates. Visited nodes should count toward the existing name suggestion limit.
