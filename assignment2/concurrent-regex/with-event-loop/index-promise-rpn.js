"use strict";

const request = require('request'),
      requestPN = require('request-promise-native'),
      myUtils = require('./utils.js');

const SHOW_ABORTED = false;

const args = myUtils.getArgs(),
      START_LINK = args.rootUrl,
      REG_EXP = args.regExp,
      MAX_REQUESTS = args.maxRequests;

const requestOptions = {  // see https://www.npmjs.com/package/request-promise
    resolveWithFullResponse: true,
    uri: START_LINK 
};

let nLinksAlreadyProcessed = 0,
    nLinksWithMatches = 0,
    averageMatches = 0,
    linksAlreadyRequested = new Set(),
    nLinksAborted = 0;

requestPN(requestOptions)
    .then(processLink)
    .catch(manageError);
linksAlreadyRequested.add(START_LINK);


// ------------------------ Functions declarations ----------------------------

function processLink(response) {
    
    if (!(response.headers['content-type']).startsWith('text/html')) {
        nLinksAborted++;
        if (SHOW_ABORTED) {
            myUtils.writeOverLastLine(`<<NOT_HTML/TEXT>> (Content type: ${response.headers['content-type']}) ${response.request.uri.href}\n`);
            myUtils.writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches);
        }

    } else {
        const linksInThisPage = myUtils.getLinks(response);
        for (let i=0; i<linksInThisPage.length; i++) {
            if (linksAlreadyRequested.size == MAX_REQUESTS) {
                break;
            }
            let link = linksInThisPage[i];
            if (!linksAlreadyRequested.has(link)) {
                requestOptions.uri = link;
                requestPN(requestOptions)
                    .then(processLink)
                    .catch(manageError);
                linksAlreadyRequested.add(link);
            }
        }
        
        const matches = myUtils.countRegexpMatches(response, REG_EXP);
        if (matches > 0) {
            averageMatches = myUtils.updateAverage(averageMatches, nLinksWithMatches, matches);
            nLinksWithMatches++;
            myUtils.writeOverLastLine(`[${matches}] ${response.request.uri.href}\n`);
        }

        nLinksAlreadyProcessed++;
        myUtils.writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches);
    }
}

function manageError(err) {  // Covers also the cases when the response code wasn't 2xx!
    nLinksAborted++;
    if (SHOW_ABORTED) {
        myUtils.writeOverLastLine(`<<ERROR>> ${err}\n`);
        myUtils.writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches);
    }
}
