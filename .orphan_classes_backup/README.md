# Orphan Classes Backup

This directory contains backup copies of orphan classes that were removed from the project.

## Removed Classes (2026-01-02):

1. **CarouselItemDecoration.kt** - RecyclerView item decoration for carousel spacing
2. **CenterCropLinearLayoutNoEffect.kt** - Custom LinearLayout with center crop background
3. **ChatOptimizationExample.kt** - Example class with no implementation
4. **FileUtil.kt** - Duplicate utility class (FileUtils.kt is the active one)

## Recovery Instructions:

To restore any class:
```bash
cp .orphan_classes_backup/20260102_114252/[ClassName].kt app/src/main/java/com/synapse/social/studioasinc/
```

For ChatOptimizationExample.kt:
```bash
cp .orphan_classes_backup/20260102_114252/ChatOptimizationExample.kt app/src/main/java/com/synapse/social/studioasinc/examples/
```

## Verification:
- All classes were verified as unused (no imports/references found)
- Project builds successfully after removal
- No XML layout references found
