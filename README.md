# BKCommonLib Region Change Tracker
Module included with BKCommonLib that tracks large-scale block changes in
other plugins like WorldEdit.

## Maven Dependency
```xml
<repositories>
    <repository>
        <id>mg-dev repo</id>
        <url>https://ci.mg-dev.eu/plugin/repository/everything</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>com.bergerkiller.bukkit.regionchangetracker</groupId>
        <artifactId>BKCommonLib-RegionChangeTracker-Core</artifactId>
        <version>1.0</version>
        <optional>true</optional>
    </dependency>
</dependencies>
```

Include _com.bergerkiller.bukkit.regionchangetracker_ in the maven shade plugin to shade it in

## Soft Depend
```yml
softdepend: [WorldEdit]
```

## Example Plugin
```java
public class MyPlugin extends JavaPlugin {
    private final RegionChangeTracker regionChangeTracker = new RegionChangeTracker(this) {
        @Override
        public void notifyChanges(World world, Collection<RegionBlockChangeChunkCoordinate> chunks) {
            // Called when there are changes done using WorldEdit/FAWE/etc.
        }
    };

    @Override
    public void onEnable() {
        regionChangeTracker.enable();
    }

    @Override
    public void disable() {
        regionChangeTracker.disable();
    }
}
```
