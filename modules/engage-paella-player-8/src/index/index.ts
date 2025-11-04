import './styles.css';


const resultsContainer = document.getElementById('results')!;
const loadingIndicator = document.getElementById('loading')!;
const pagination = document.getElementById('pagination')!;


// Global variables for pagination and results
let currentPage: number = 1;
let pageSize: number = 10;
let currentSeries: string | null = null;
let filteredResults = [];
let totalVideos: number = 0;


const USE_OC_SERVER_FROM_URL = import.meta.env.USE_OC_SERVER_FROM_URL === 'true' || false;
const OC_PRESENTATION_URL = getOcPresentationUrl();
const OC_PAELLA8_URL_TEMPLATE = import.meta.env.OC_PAELLA8_URL_TEMPLATE || getUrlFromBase(OC_PRESENTATION_URL, '/play/{videoId}');


function getOcPresentationUrl(): string {
    let url = import.meta.env.OC_PRESENTATION_URL || '/';
    
    if (USE_OC_SERVER_FROM_URL) {    
        const params = new URLSearchParams(window.location.search);
        const ocServerUrl = params.get('ocServer');
        if (ocServerUrl) {
            url = ocServerUrl;
        }
    }
    
    return url;
}

// Utility to format strings with parameters
function formatString(base: string, params: Record<string, string>): string {
    let formatted = base;
    for (const [key, value] of Object.entries(params)) {
        formatted = formatted.replace(`{${key}}`, encodeURIComponent(value));
    }
    return formatted;
}
 

// Utility to get URL from base and path
function getUrlFromBase(base: string, path: string): string {
    if (!base) return path
    if (!path) return base.endsWith('/') ? base.slice(0, -1) : base;

    const normalizedBase = base.endsWith('/') ? base.slice(0, -1) : base;
    const normalizedPath = path.length == 0 ? '' : (path.startsWith('/') ? path : `/${path}`);
    return `${normalizedBase}${normalizedPath}`;
}

// Utility to read query parameters
function getQueryParams() {
    const params = new URLSearchParams(window.location.search);
    let pageSize = parseInt(params.get('pageSize') ?? '10') || 10;
    let page = parseInt(params.get('page') ?? '1') || 1;
    let series = params.get('series') || "";
    return {
        pageSize,
        page,
        series
    };
}
// Utility functions to ensure array or single value
function ensureArray(thing: any) {
    return thing
        ? (Array.isArray(thing)
            ? thing
            : [thing])
        : [];
}
// Ensure single value from array or single item
function ensureSingle(thing: any) {
    return (Array.isArray(thing)
        ? thing[0]
        : thing);
}


// Simulate series info
// function getSeriesInfo(series: string ) {
//     if (!series) return { title: "Video Results" };
//     // Customize this according to real logic
//     return { title: `Series: ${series}` };
// }




async function fetchDescription(video: any, _i: number): Promise<string> {
    let descp = ensureSingle(video?.dc?.description) as string | undefined;
    if (!descp) {
        const attch = getAttachment(video, ["ai/summary"]);
        if (attch) {
            const summary = await fetch(attch.url)
                .then(response => response.text())
                .catch(() => null);
            if (summary) {
                descp = summary;
            }
        }
    }

    return descp ?? '';
}

function getAttachment(video: any, types: string[]) {
    if (!video || !video.mediapackage || !video.mediapackage.attachments) return null;
    const attachments = ensureArray(video.mediapackage.attachments.attachment);
    for (const type of types) {
        const attachment = attachments.find(a => a.type === type);
        if (attachment) return attachment;
    }
    return null;
}


function getTimeline(video: any) {
    let timeline = "";
    let grid = "10x10";

    const attch = getAttachment(video, ["presentation/timeline+preview", "presenter/timeline+preview"]);
    if (attch != null) {
        timeline = attch.url;
        let isX = 10;
        let isY = 10;
        attch.additionalProperties.property.forEach((p: any) => {
            if (p.name === "imageSizeX") {
                isX = p.value
            }
            else if (p.name === "imageSizeY") {
                isY = p.value
            }
        });

        grid = `${isX}x${isY}`;
    }

    return {
        timeline: timeline,
        grid: grid
    };
}

// Fectch videos with pagination and series id (sid)
async function fetchVideos(page: number, pageSize: number, sid: string) {
    const url = getUrlFromBase(OC_PRESENTATION_URL, `/search/episode.json?limit=${pageSize}&offset=${pageSize * (page - 1)}&sid=${sid}&sort=created%20desc`)
    const searchResults = await fetch(url)
        .then(response => response.json())
        .catch(error => {
            console.error("Error fetching search results:", error);
            return { total: 0, result: [] };
        });


    const total = searchResults.total ?? 0;
    const results = [];
    if (Array.isArray(searchResults.result)) {
        for (let i = 0; i < searchResults.result.length; i++) {
            const video = searchResults.result[i];
            const description = await fetchDescription(video, i);
            const { timeline, grid } = getTimeline(video);
            const preview = getAttachment(video, ["presentation/search+preview", "presenter/search+preview"])?.url;
            
            
            const playerUrl = formatString(OC_PAELLA8_URL_TEMPLATE, {
                videoId: video?.mediapackage?.id ?? video?.id,
                OC_PRESENTATION_URL: OC_PRESENTATION_URL
            });
                        
            results.push({
                url: playerUrl,
                title: video.mediapackage.title,
                author: ensureArray(video?.mediapackage?.creators?.creator).join(', '),
                date: video?.mediapackage?.start
                    ? new Date(video.mediapackage.start).toLocaleString(undefined, {
                        dateStyle: 'medium',
                        timeStyle: 'short'
                    })
                    : "",
                description,
                preview,
                timeline,
                grid,
                duration: (video?.mediapackage?.duration ?? 0) / 1000
            });
        }
    }

    return {
        results,
        total
    };
}



function renderSkeletons(count: number) {
    const skeletons = [];
    for (let i = 0; i < count; i++) {
        skeletons.push(`
                    <div class="bg-white rounded-xl shadow p-4 flex flex-col md:flex-row gap-4">
                        <div class="w-full md:w-1/3 aspect-video rounded-md animate-pulse-fast bg-gradient-to-r from-gray-100 via-gray-300 to-gray-400"></div>
                        <div class="flex-1 space-y-3">
                            <div class="h-6 rounded w-2/3 animate-pulse-fast bg-gradient-to-r from-gray-100 via-gray-300 to-gray-400"></div>
                            <div class="h-4 rounded w-1/3 animate-pulse-fast bg-gradient-to-r from-gray-100 via-gray-300 to-gray-400"></div>
                            <div class="h-4 rounded w-full animate-pulse-fast bg-gradient-to-r from-gray-100 via-gray-300 to-gray-400"></div>
                            <div class="h-4 rounded w-5/6 animate-pulse-fast bg-gradient-to-r from-gray-100 via-gray-300 to-gray-400"></div>
                        </div>
                    </div>
                `);
    }
    const loadingSkeletons = document.getElementById('loading-skeletons')
    if (loadingSkeletons) {
        loadingSkeletons.innerHTML = skeletons.join('');
    }
}

function showLoading(show: boolean) {
    loadingIndicator.classList.toggle('hidden', !show);
    if (show) {
        // Hide results while loading
        resultsContainer.innerHTML = '';
        renderSkeletons(pageSize || 10);
    }
}

function renderPagination(totalPages: number) {
    pagination.innerHTML = '';
    const maxButtons = 10;
    let start = 0;
    let end = totalPages;

    // Adjust for page base 1
    const pageIdx = currentPage - 1;

    if (totalPages > maxButtons) {
        if (pageIdx <= 4) {
            start = 0;
            end = maxButtons;
        } else if (pageIdx >= totalPages - 5) {
            start = totalPages - maxButtons;
            end = totalPages;
        } else {
            start = pageIdx - 4;
            end = pageIdx + 5 + 1;
        }
    }

    // First button
    if (start > 0) {
        addPageButton(1);
        if (start > 1) addEllipsis();
    }

    for (let i = start; i < end && i < totalPages; i++) {
        // Always show current, previous and next page
        if (
            i === pageIdx ||
            i === pageIdx - 1 ||
            i === pageIdx + 1 ||
            (i >= start && i < start + 2) ||
            (i >= end - 2 && i < end)
        ) {
            addPageButton(i + 1);
        } else if (
            i === start + 2 && pageIdx > 5
        ) {
            addEllipsis();
        } else if (
            i === end - 3 && pageIdx < totalPages - 6
        ) {
            addEllipsis();
        }
    }

    // Last button
    if (end < totalPages) {
        if (end < totalPages - 1) addEllipsis();
        addPageButton(totalPages);
    }

    function addPageButton(pageNum: number) {
        const btn = document.createElement('button');
        btn.textContent = `${pageNum}`;
        btn.className = `px-4 py-2 border rounded ${pageNum === currentPage ? 'bg-blue-500 text-white' : 'bg-white hover:bg-gray-200'}`;
        btn.onclick = () => {
            const url = new URL(window.location.href);
            url.searchParams.set('page', `${pageNum}`);
            window.history.replaceState({}, '', url);
            loadPage(pageNum);
        };
        pagination.appendChild(btn);
    }

    function addEllipsis() {
        const span = document.createElement('span');
        span.textContent = '...';
        span.className = 'px-2 py-2 text-gray-400 select-none';
        pagination.appendChild(span);
    }

    // Show range info
    const info = document.getElementById('pagination-info');
    if (info) {
        if (totalVideos === 0) {
            info.textContent = "No results";
        }
        else {
            const from = totalVideos === 0 ? 0 : ((currentPage - 1) * pageSize) + 1;
            const to = Math.min(currentPage * pageSize, totalVideos);
            info.textContent = `Showing results ${from} to ${to} of ${totalVideos}`;
        }
    }
}

// Format seconds to human readable (e.g. 1:23:45 or 12:34)
function formatDuration(seconds: number): string {
    seconds = Math.floor(seconds);
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) {
        return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    } else {
        return `${m}:${s.toString().padStart(2, '0')}`;
    }
}

function renderResults(videos: any[]) {
    resultsContainer.innerHTML = '';
    videos.forEach(video => {
        const hasTimeline = !!video.timeline;

        const result = document.createElement('a');
        result.href = video.url;
        result.className = `cursor-pointer bg-white rounded-xl shadow hover:shadow-lg transition-shadow duration-300 overflow-hidden flex flex-col md:flex-row gap-4 p-4 group`;
        result.dataset.grid = video.grid;
        result.dataset.timeline = video.timeline;

        result.innerHTML = `
      <div class="w-full md:w-1/3 aspect-video overflow-hidden rounded-md relative preview-container">
        <img 
          src="${video.preview}" 
          alt="Preview" 
          class="w-full object-cover transition duration-300 absolute inset-0 z-10 preview"
        />
        <div class="absolute inset-0 z-0 hidden timeline-sprite"></div>
        <div class="absolute left-0 bottom-0 h-1 bg-red-800 opacity-90 z-20 timeline-progress-bar" style="width:0"></div>
      </div>
      <div class="flex-1 space-y-2">
        <h2 class="text-lg font-semibold text-blue-700 group-hover:underline ">${video.title}</h2>
        <div class="text-sm text-gray-600 flex flex-wrap items-center gap-2">
          <span>${video.author}</span>
          <span>&middot;</span>
          <span>${video.date}</span>
          <span>&middot;</span>
          <span class="text-gray-500" title="Duration">${formatDuration(video.duration)}</span>
        </div>
        <p class="text-gray-700  line-clamp-6 overflow-y-auto">
          ${video.description}
        </p>
      </div>
    `;

        if (!hasTimeline) {
            result.classList.add('no-timeline');
        }

        resultsContainer?.appendChild(result);
    });

    setTimeout(() => applyTimelineHover(), 100);
}

function applyTimelineHover() {
    document.querySelectorAll<HTMLElement>('.group').forEach((group) => {
        const hasTimeline = !!group.dataset.timeline && group.dataset.timeline !== "";
        const previewContainer = group.querySelector<HTMLElement>('.preview-container');
        const preview = group.querySelector<HTMLElement>('.preview');
        const timeline = group.querySelector<HTMLElement>('.timeline-sprite');
        const progressBar = group.querySelector<HTMLElement>('.timeline-progress-bar');

        // 3D tilt effect for all videos (timeline or not)
        if (previewContainer) {
            previewContainer.addEventListener('mouseenter', () => {
                previewContainer.classList.add('tilted');
            });
            previewContainer.addEventListener('mousemove', () => {
                previewContainer.classList.add('tilted');
            });
            previewContainer.addEventListener('mouseleave', () => {
                previewContainer.classList.remove('tilted');
            });
        }

        if (hasTimeline) {
            const timelineUrl = group.dataset.timeline;
            const [cols, rows] = group.dataset.grid?.split('x').map(n => parseInt(n))  || [10, 10];
            if (!preview || !timeline || !timelineUrl || !cols || !rows) return;

            timeline.style.backgroundImage = `url('${timelineUrl}')`;
            timeline.style.backgroundSize = `${cols * 100}% ${rows * 100}%`;

            previewContainer?.addEventListener('mousemove', e => {
                const bounds = preview.getBoundingClientRect();
                const x = e.clientX - bounds.left;
                const frameIndex = Math.floor((x / bounds.width) * (cols * rows));
                const col = frameIndex % cols;
                const row = Math.floor(frameIndex / cols);

                timeline.style.backgroundPosition = `${-col * 100}% ${-row * 100}%`;
                timeline.classList.remove('hidden');
                preview.classList.add('opacity-0');
                timeline.classList.add('tilted');

                // Progress bar: show and set width
                if (progressBar) {
                    const percent = Math.max(0, Math.min(1, x / bounds.width));
                    progressBar.style.width = `${percent * 100}%`;
                    progressBar.style.opacity = "1";
                }
            });

            previewContainer?.addEventListener('mouseleave', () => {
                timeline.classList.add('hidden');
                preview.classList.remove('opacity-0');
                timeline.classList.remove('tilted');
                // Hide progress bar
                if (progressBar) {
                    progressBar.style.width = "0";
                    progressBar.style.opacity = "0";
                }
            });
        }
    });
}

// Use query parameters for loading page
async function loadPage(page: number = 1) {
    showLoading(true);

    setTimeout(async () => {
        // Read query parameters every time (in case they change)
        const params = getQueryParams();
        pageSize = params.pageSize;
        currentPage = typeof page === "number" ? page : params.page;
        currentSeries = params.series;
        
        const search = await fetchVideos(currentPage, pageSize, currentSeries);
        filteredResults = search.results;
        totalVideos = search.total;

        // Series info
        // const seriesInfo = getSeriesInfo(currentSeries);
        // document.getElementById('series-title').textContent = seriesInfo.title;

        // Pagination
        const totalPages = Math.ceil(totalVideos / pageSize);
        renderResults(filteredResults);
        renderPagination(totalPages);

        showLoading(false);
    }, 1);
}

window.addEventListener('DOMContentLoaded', () => loadPage(1));