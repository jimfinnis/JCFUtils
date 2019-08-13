# Region system
I'd quite like to have a system for labelled regions. In this, the big
problem is making the operation for getting a list of regions from block
coordinates as quick as possible without storing lots of data (note that
a block can be in several overlapping regions).

The obvious thing to do is chunking - divide the map into chunks (not 
necessarily aligned with MC's native chunks) and for each chunk keep a
list of regions which intersect it. This is good, but if the chunks
are too large we'll end up with a lot of small regions (like rooms in
a castle) inside one chunk, which is inefficient. If the chunks are too
small, we end up with the opposite problem: a very large region (say,
a country or continent) will end up creating a huge amount of chunks
with one region in them.

The proposed solution is to do this at multiple levels: we 
maintain N sets of chunks of type ```ChunkSet```, each of different
granularity. For example, 16x16, 64x64, 1024x1024. Note that the chunks
use XZ only; the Y coordinate is handled at the region level.
Each set has a
method ```getRegionsAt()``` which takes blocks coords and returns
a list of regions intersecting that chunk.

The chunksets live in list, sorted by increasing granularity (i.e.
smallest size last).

## Code operations
### Adding a region, given an AABB
The aim here is to add the region to the largest chunkset within
which it is not completely contained within one chunk. This avoids
small regions being added to the huge chunkset.
```
foreach set in chunksets:
    if not(aabb contained entirely within one chunk at this granularity AND
        this is not the smallset chunkset size)
        get chunk coordinates for southwest and northeast corners of aabb
        for each chunk coordinate within the above:
            get or create chunk
            add region to chunk
    add region to master list (see "persistent data" below)
```            

### Getting a region, given the block coords
Note the use of the link field here: that's why s is a set.
```
    s := empty set
    foreach set in chunksets:
        c := chunk containing coords (or null) from s
        if c is not null and c contains coords:
            foreach region r in c:
                if coords in r:
                    if r is a link:
                        r = parent of r (i.e. follow link)
                    if r not in s:
                        add r to s
    (possibly) convert s into a list and sort by size.
    return s
```

## Linked regions
Sometimes regions will have irregular shapes made up of several
cuboids. In this case, one region will be the "nominal" region.
Other regions will link to this region using a link ID field,
so getRegion will return the linked region.

## Data in a region

* Name (obviously) (unused if link ID is nonzero)
* Unique integer ID starting at 1 (so we can change the name)
* (possibly) a region link ID or zero if there is no link
* XYZ coords of corners
* size

## Persistent data
Data for regions is stored in a yaml file (or some persistence model).
It does not contain chunk data; regions are added to chunks on load.
This implies that there must be a master list of all regions for saving.


## User operations        
    
* adding a region by specifying two corners, perhaps by right-clicking on
blocks or placing a special block? Requires some thought, this one.
* adding a link region to the region previously added (even if that was a link
too)
* commands:
** display the regions and their IDs
** to change region name
** delete region
** remove link (and set name)
** add link

## Region display

Not sure how to do this. We could use Bukkit's ```sendTitle```, or
we could just send a chat message.
