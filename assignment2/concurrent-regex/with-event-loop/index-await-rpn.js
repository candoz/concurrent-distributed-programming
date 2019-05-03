"use strict";

const request = require('request'),
      requestPN = require('request-promise-native'),
      myUtils = require('./utils.js');

const SHOW_ABORTED = false;

main();


// ------------------------ Functions declarations ----------------------------

async function main() {

    const args = myUtils.getArgs(),
          START_LINK = args.rootUrl,
          REG_EXP = args.regExp,
          MAX_REQUESTS = args.maxRequests;

    const requestOptions = {  // see https://www.npmjs.com/package/request-promise
        resolveWithFullResponse: true 
    };

    let nLinksWithMatches = 0,
        averageMatches = 0,
        linksAlreadyRequested = new Set(),
        linksToRequest = new Array(),
        nLinksAborted = 0;

    linksToRequest.push(START_LINK);

    while (linksAlreadyRequested.size < MAX_REQUESTS && linksToRequest.length > 0) {
        
        const currentLink = linksToRequest[0];
        linksToRequest.shift();  // remove the first element (FIFO for a Breadth-First-like approach)
        requestOptions.uri = currentLink;
        
        try {
            const response = await requestPN(requestOptions);
            linksAlreadyRequested.add(currentLink);
            
            if (!(response.headers['content-type']).startsWith('text/html')) {
                nLinksAborted++;
                if (SHOW_ABORTED) {
                    myUtils.writeOverLastLine(`<<NOT_HTML/TEXT>> (Content type: ${response.headers['content-type']}) ${response.request.uri.href}\n`);
                    myUtils.writeStats(linksAlreadyRequested.size, nLinksAborted, nLinksWithMatches, averageMatches);
                }
            
            } else {  // if everything is OK
                const result = processLink(response, REG_EXP);
                const linksFound = result.links;
                for (let i=0; i<linksFound.length; i++) {
                    if (linksAlreadyRequested.size + linksToRequest.length == MAX_REQUESTS) {
                        break;
                    }
                    if (!linksAlreadyRequested.has(linksFound[i]) && !linksToRequest.includes(linksFound[i])) {
                        linksToRequest.push(linksFound[i]);
                    }
                }

                if (result.matches > 0) {
                    averageMatches = myUtils.updateAverage(averageMatches, nLinksWithMatches, result.matches);
                    nLinksWithMatches++;
                    myUtils.writeOverLastLine(`[${result.matches}] ${currentLink}\n`);
                }

                myUtils.writeStats(linksAlreadyRequested.size, nLinksAborted, nLinksWithMatches, averageMatches);
            }

        } catch(err) {  // Covers also the cases when the response code wasn't 2xx!
            nLinksAborted++;
            if (SHOW_ABORTED) {
                myUtils.writeOverLastLine(`<<ERROR>> ${err}\n`);
                myUtils.writeStats(linksAlreadyRequested.size, nLinksAborted, nLinksWithMatches, averageMatches);
            }
        }
    }
}

function processLink(response, regExp) {
    const links = myUtils.getLinks(response);
    const matches = myUtils.countRegexpMatches(response, regExp);
    return {'links': links, 'matches': matches};
}
