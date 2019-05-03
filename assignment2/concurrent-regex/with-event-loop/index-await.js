"use strict";

const request = require('request'),
    cheerio = require('cheerio'),
    myUtils = require('./utils.js');

let nLinksAlreadyProcessed = 0,
    nLinksWithMatches = 0,
    averageMatches = 0,
    linksAlreadyRequested = new Set(),
    linksFound = new Array(),
    nLinksAborted = 0;

const args = myUtils.getArgs(),
    START_LINK = (args.rootUrl),
    REG_EXP = args.regExp,
    MAX_REQUESTS = args.maxRequests;

const SHOW_ABORTED = true;


linksFound.push(START_LINK);
webCrawling();
linksAlreadyRequested.add(START_LINK);

// ------------------------ Functions declarations ----------------------------

async function webCrawling(){
    var link = linksFound.pop();
    if (linksAlreadyRequested.size < MAX_REQUESTS && !(linksAlreadyRequested.has(link))) {
        try {
            var crawled = await visitLinks(link);
        }catch(err){
            console.log(`\n\nError:\n ${err}\n`);
        }
    }else{
        console.log("Reached max number of requests");
    }
}

function visitLinks(url){
    return new Promise((resolve, reject) => {
        
        request(url, (error, response, body) => {
            if (response){
                const responseCode = response.statusCode;
                const contentType = response.headers['content-type'];
                if(responseCode != 200) {
                    nLinksAborted++;
                    if (SHOW_ABORTED) {
                        myUtils.writeOverLastLine(`<<REJECTED>> (Code: ${responseCode}) ${response.request.uri.href}\n`);
                        myUtils.writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches);
                    }
                } else if (!contentType.startsWith('text/html')) {
                    nLinksAborted++;
                    if (SHOW_ABORTED) {
                        myUtils.writeOverLastLine(`<<NOT_HTML/TEXT>> (Content type: ${contentType}) ${response.request.uri.href}\n`);
                        myUtils.writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches);
                    }
                }else{
                    const linksInThisPage = myUtils.getLinks(response);
                    for (let i=0; i<linksInThisPage.length; i++) {
                        if (linksAlreadyRequested.size == MAX_REQUESTS) {
                            break;
                        }
                        let link = linksInThisPage[i];
                        if (!linksAlreadyRequested.has(link)) {
                            linksFound.push(link);
                            webCrawling();
                            linksAlreadyRequested.add(link);
                        }
                    }
                    
                    const matches = myUtils.countRegexpMatches(response, REG_EXP);
                    if (matches > 0) {
                        nLinksWithMatches++;
                        averageMatches = myUtils.updateAverage(averageMatches, nLinksWithMatches, matches);
                        myUtils.writeOverLastLine(`[${matches}] ${response.request.uri.href}\n`);
                    }
                    nLinksAlreadyProcessed++;
                    myUtils.writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches);       
                    resolve(response);
                }
            }else if(error){
                reject(error);
            }

        });
    });
}

