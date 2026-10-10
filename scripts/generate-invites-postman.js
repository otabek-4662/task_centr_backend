const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const rootDir = path.resolve(__dirname, '..');
const openapiPath = path.join(rootDir, 'docs', 'openapi.json');
const tempPostmanPath = path.join(rootDir, 'target', 'full-postman.json');
const outputPath = path.join(rootDir, 'docs', 'invites.postman_collection.json');

console.log('Converting docs/openapi.json using openapi-to-postmanv2...');
execSync(`npx -y openapi-to-postmanv2 -s "${openapiPath}" -o "${tempPostmanPath}" -p`, {
    stdio: 'inherit'
});

console.log('Reading generated collection...');
const fullCollection = JSON.parse(fs.readFileSync(tempPostmanPath, 'utf8'));

// Helper function to find items related to invitations
function findInvitationItems(items) {
    let result = [];
    for (const item of items) {
        if (item.item && Array.isArray(item.item)) {
            // Check if this folder or its children are invitations
            if (item.name === 'invitations' || item.name === 'invites') {
                result.push(item);
            } else {
                const sub = findInvitationItems(item.item);
                if (sub.length > 0) {
                    result.push({
                        name: item.name,
                        description: item.description,
                        item: sub
                    });
                }
            }
        } else if (item.request) {
            const urlStr = JSON.stringify(item.request.url || '');
            if (urlStr.includes('invit')) {
                result.push(item);
            }
        }
    }
    return result;
}

const inviteItems = findInvitationItems(fullCollection.item || []);

const cleanCollection = {
    info: {
        _postman_id: "task-center-invites-v2",
        name: "Task Center — Workspace Invitations API",
        description: "Generated from docs/openapi.json via openapi-to-postmanv2. Contains all Workspace Invitation endpoints with {{baseUrl}} and {{jwt}}.",
        schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    variable: [
        {
            key: "baseUrl",
            value: "http://localhost:8080",
            type: "string"
        },
        {
            key: "jwt",
            value: "",
            type: "string",
            description: "Bearer JWT token (do not commit secrets)"
        },
        {
            key: "workspaceId",
            value: "7c9e6679-7425-40de-944b-e07fc1f90ae7",
            type: "string"
        },
        {
            key: "invitationId",
            value: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            type: "string"
        },
        {
            key: "inviteToken",
            value: "sample_token_32bytes_urlsafe",
            type: "string"
        }
    ],
    item: inviteItems.length > 0 ? inviteItems : fullCollection.item
};

// Remove any hardcoded authorization secrets from requests and use {{jwt}}
function cleanAuth(items) {
    for (const item of items) {
        if (item.item && Array.isArray(item.item)) {
            cleanAuth(item.item);
        } else if (item.request) {
            if (item.request.auth && item.request.auth.bearer) {
                item.request.auth.bearer = [
                    {
                        key: "token",
                        value: "{{jwt}}",
                        type: "string"
                    }
                ];
            }
            if (item.request.header && Array.isArray(item.request.header)) {
                item.request.header.forEach(h => {
                    if (h.key && h.key.toLowerCase() === 'authorization') {
                        h.value = 'Bearer {{jwt}}';
                    }
                });
            }
        }
    }
}
cleanAuth(cleanCollection.item);

fs.writeFileSync(outputPath, JSON.stringify(cleanCollection, null, 2), 'utf8');
console.log('Successfully generated docs/invites.postman_collection.json from openapi.json!');
