#!/bin/bash

# List of branches to merge (excluding main and develop)
branches=(
    "chat-realtime-subscription-15000878287643291846"
    "chore-secure-onesignal-credentials-18066842922492748230"
    "feature-message-forwarding-ui-16956452752557662567"
    "feature-oauth-support-13637135772101466837"
    "jules-chat-ui-updater-fix-38845512859311166"
    "migrate-chat-to-compose-m3-15340222604768733854"
    "notification-system-update-1491176133416946606"
    "realtime-read-receipts-10304203547174324374"
    "reels-adapter-init-14435374100296209888"
    "refactor-posts-adapter-fetching-8170451185715568676"
)

# Ensure we're on develop branch
git checkout develop

echo "Starting merge process for ${#branches[@]} branches..."

successful_merges=()
failed_merges=()

for branch in "${branches[@]}"; do
    echo "----------------------------------------"
    echo "Attempting to merge: $branch"
    
    # Try to merge the branch
    if git merge "origin/$branch" --no-edit; then
        echo "✅ Successfully merged: $branch"
        successful_merges+=("$branch")
    else
        echo "❌ Failed to merge: $branch (conflicts detected)"
        failed_merges+=("$branch")
        # Abort the merge to clean up
        git merge --abort
    fi
done

echo "========================================"
echo "MERGE SUMMARY:"
echo "========================================"
echo "Successful merges (${#successful_merges[@]}):"
for branch in "${successful_merges[@]}"; do
    echo "  ✅ $branch"
done

echo ""
echo "Failed merges (${#failed_merges[@]}):"
for branch in "${failed_merges[@]}"; do
    echo "  ❌ $branch"
done

if [ ${#successful_merges[@]} -gt 0 ]; then
    echo ""
    echo "Pushing merged changes to origin/develop..."
    git push origin develop
fi
