#!/bin/bash

# Update all import statements to use domain.model instead of model
cd /home/Ashik/android/app/src/main/java/com/synapse/social/studioasinc

echo "Updating import statements..."

# Update imports from model to domain.model
find . -name "*.kt" -exec sed -i 's/import com\.synapse\.social\.studioasinc\.model\./import com.synapse.social.studioasinc.domain.model./g' {} \;

# Update imports from home.User to domain.model.User
find . -name "*.kt" -exec sed -i 's/import com\.synapse\.social\.studioasinc\.home\.User/import com.synapse.social.studioasinc.domain.model.User/g' {} \;

# Update any remaining references to the old model package
find . -name "*.kt" -exec sed -i 's/com\.synapse\.social\.studioasinc\.model\./com.synapse.social.studioasinc.domain.model./g' {} \;

# Update any remaining references to home.User
find . -name "*.kt" -exec sed -i 's/com\.synapse\.social\.studioasinc\.home\.User/com.synapse.social.studioasinc.domain.model.User/g' {} \;

echo "Import statements updated successfully"
