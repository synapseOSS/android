#!/bin/bash

# Update package declarations for moved model files
cd /home/Ashik/android/app/src/main/java/com/synapse/social/studioasinc

# Update main model files
find domain/model -name "*.kt" -not -path "*/models/*" -exec sed -i 's/package com\.synapse\.social\.studioasinc\.model/package com.synapse.social.studioasinc.domain.model/g' {} \;

# Update models subdirectory files
find domain/model/models -name "*.kt" -exec sed -i 's/package com\.synapse\.social\.studioasinc\.model\.models/package com.synapse.social.studioasinc.domain.model.models/g' {} \;

echo "Updated package declarations for all moved model files"
