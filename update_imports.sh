#!/bin/bash

# Update backend imports to data.remote.services
find /home/Ashik/android/app/src/main/java -name "*.kt" -exec sed -i 's/import com\.synapse\.social\.studioasinc\.backend\./import com.synapse.social.studioasinc.data.remote.services./g' {} \;

# Update backend.interfaces imports to data.remote.services.interfaces
find /home/Ashik/android/app/src/main/java -name "*.kt" -exec sed -i 's/import com\.synapse\.social\.studioasinc\.data\.remote\.services\.interfaces\./import com.synapse.social.studioasinc.data.remote.services.interfaces./g' {} \;

# Update backend.dto imports to data.remote.dto
find /home/Ashik/android/app/src/main/java -name "*.kt" -exec sed -i 's/import com\.synapse\.social\.studioasinc\.data\.remote\.services\.dto\./import com.synapse.social.studioasinc.data.remote.dto./g' {} \;

# Update data.local imports to data.local.database (but not data.local.database.* which should stay)
find /home/Ashik/android/app/src/main/java -name "*.kt" -exec sed -i 's/import com\.synapse\.social\.studioasinc\.data\.local\.\([^d][^a][^t][^a][^b][^a][^s][^e]\)/import com.synapse.social.studioasinc.data.local.database.\1/g' {} \;

# Handle specific cases for data.local.database imports that might have been double-converted
find /home/Ashik/android/app/src/main/java -name "*.kt" -exec sed -i 's/import com\.synapse\.social\.studioasinc\.data\.local\.database\.database\./import com.synapse.social.studioasinc.data.local.database./g' {} \;

echo "Import statements updated successfully!"
