#!/bin/bash

# Update package declarations and imports for moved auth files

# Update main UI screens
sed -i 's/package com\.synapse\.social\.studioasinc\.ui\.auth/package com.synapse.social.studioasinc.feature.auth.ui/g' \
  /home/Ashik/android/app/src/main/java/com/synapse/social/studioasinc/feature/auth/ui/*.kt

# Update imports in UI screens
sed -i 's/com\.synapse\.social\.studioasinc\.ui\.auth\./com.synapse.social.studioasinc.feature.auth.ui./g' \
  /home/Ashik/android/app/src/main/java/com/synapse/social/studioasinc/feature/auth/ui/*.kt

# Update package declarations in models
sed -i 's/package com\.synapse\.social\.studioasinc\.ui\.auth\.models/package com.synapse.social.studioasinc.feature.auth.ui.models/g' \
  /home/Ashik/android/app/src/main/java/com/synapse/social/studioasinc/feature/auth/ui/models/*.kt

# Update package declarations in components
sed -i 's/package com\.synapse\.social\.studioasinc\.ui\.auth\.components/package com.synapse.social.studioasinc.feature.auth.ui.components/g' \
  /home/Ashik/android/app/src/main/java/com/synapse/social/studioasinc/feature/auth/ui/components/*.kt

# Update imports in components
sed -i 's/com\.synapse\.social\.studioasinc\.ui\.auth\./com.synapse.social.studioasinc.feature.auth.ui./g' \
  /home/Ashik/android/app/src/main/java/com/synapse/social/studioasinc/feature/auth/ui/components/*.kt

# Update package declarations in util
sed -i 's/package com\.synapse\.social\.studioasinc\.ui\.auth\.util/package com.synapse.social.studioasinc.feature.auth.ui.util/g' \
  /home/Ashik/android/app/src/main/java/com/synapse/social/studioasinc/feature/auth/ui/util/*.kt

# Update AuthViewModel package
sed -i 's/package com\.synapse\.social\.studioasinc\.ui\.auth/package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel/g' \
  /home/Ashik/android/app/src/main/java/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/AuthViewModel.kt

# Update imports in AuthViewModel
sed -i 's/com\.synapse\.social\.studioasinc\.ui\.auth\./com.synapse.social.studioasinc.feature.auth.ui./g' \
  /home/Ashik/android/app/src/main/java/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/AuthViewModel.kt

echo "Package declarations and imports updated successfully!"
