import { Pagination, type PaginationOutput } from "./common.type"

export class RequestClient {
    constructor(
        private readonly baseUrl: string,
        private readonly getAccessToken: () => Promise<string>
    ) {}

    async post<B, R>(path: string, body: B): Promise<R>  {
        return await this.makeRequest(HttpMethod.POST, path, body)
    }

    private async makeRequest<B, T>(httpMethod: HttpMethod, path: string, body?: B): Promise<T> {
        // Only the path: the bodies and the query params carry personal data (e.g. a CPF), which stays out of the console
        console.info(`Making a ${httpMethod} http request to ${this.baseUrl + path.split("?")[0]}`)

        const response = await fetch(this.baseUrl + path, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${await this.getAccessToken()}`
            },
            mode: "cors",
            method: httpMethod,
            body: JSON.stringify(body)
        })

        const responseBodyText = await response.text()

        if (!response.ok) {
            const problemDetail = this.parseProblemDetail(responseBodyText)
            // The problem detail of the core-api names the fields involved, never the values sent
            console.error(`Error: ${response.status} ${problemDetail.type ?? ""} ${problemDetail.detail ?? ""}`)
            throw new HttpError(response.status, problemDetail)
        }

        return JSON.parse(responseBodyText)
    }

    // The core-api answers every error with a problem detail (RFC 9457), but a proxy in the way may answer with HTML
    private parseProblemDetail(responseBodyText: string): ProblemDetail {
        try {
            return JSON.parse(responseBodyText)
        } catch {
            return {}
        }
    }

    async get<R>(path: string, queryParams: Map<string, string> = new Map()): Promise<R>  {
        const query = new URLSearchParams([...queryParams]).toString()
        return await this.makeRequest(HttpMethod.GET, query ? `${path}?${query}` : path)
    }

    async put<B, R>(path: string, body: B): Promise<R>  {
        return await this.makeRequest(HttpMethod.PUT, path, body)
    }

    async delete<R>(path: string): Promise<R>  {
        return await this.makeRequest(HttpMethod.DELETE, path)
    }
}

enum HttpMethod {
    POST = "POST",
    GET = "GET",
    PUT = "PUT",
    DELETE = "DELETE"
}

export interface ProblemDetail {
    readonly type?: string
    readonly title?: string
    readonly detail?: string
    readonly errors?: Array<InvalidField>
}

export interface InvalidField {
    readonly field: string
    readonly message: string
}

export class HttpError extends Error {
    constructor(
        public readonly status: number,
        public readonly problemDetail: ProblemDetail
    ) {
        super(problemDetail.detail ?? `Http error ${status}`)
    }
}

// The largest pageSize the core-api accepts
const MAX_PAGE_SIZE = 50

/**
 * A resource of the core-api that can be created, listed, read and deleted. T is the type the dashboard works with,
 * O is the output of the API that becomes T, C is the input of the creation and F are the filters of the listing.
 */
export class ResourceService<T, O, C, F extends object = object> {
    constructor(
        protected readonly requestClient: RequestClient,
        protected readonly path: string,
        protected readonly from: (output: O) => T
    ) {}

    async create(input: C): Promise<T> {
        console.info(`Creating ${this.path}`)
        const output = await this.requestClient.post<C, O>(this.path, input)
        console.info("Success")
        return this.from(output)
    }

    async getAll(page: number, pageSize: number, filters?: F): Promise<Pagination<T>> {
        console.info(`Getting ${this.path}`)
        console.debug(`Page: ${page}`)
        console.debug(`Page size: ${pageSize}`)
        const queryParams = new Map<string, string>()
        queryParams.set("page", page.toString())
        queryParams.set("pageSize", pageSize.toString())

        for (const [name, value] of Object.entries(filters ?? {})) {
            if (value !== undefined) {
                console.debug(`Filter: ${name}`)
                queryParams.set(name, String(value))
            }
        }

        const paginationOutput = await this.requestClient.get<PaginationOutput<O>>(this.path, queryParams)
        console.info("Success")
        return Pagination.from(paginationOutput, this.from)
    }

    // Every record of the listing, such as the options of a Select: reads the first page, then the others together
    async getAllPages(filters?: F): Promise<Array<T>> {
        const firstPage = await this.getAll(0, MAX_PAGE_SIZE, filters)
        const otherPages = await Promise.all(
            Array.from({ length: Math.max(firstPage.totalPages - 1, 0) }, (_, i) => this.getAll(i + 1, MAX_PAGE_SIZE, filters))
        )
        return [firstPage, ...otherPages].flatMap(page => page.content)
    }

    async getById(id: string): Promise<T> {
        console.info(`Getting ${this.path}/${id}`)
        const output = await this.requestClient.get<O>(`${this.path}/${id}`)
        console.info("Success")
        return this.from(output)
    }

    async deleteById(id: string): Promise<T> {
        console.info(`Deleting ${this.path}/${id}`)
        const output = await this.requestClient.delete<O>(`${this.path}/${id}`)
        console.info("Success")
        return this.from(output)
    }
}

// A resource that can also be updated. U is the input of the update.
export class CrudService<T, O, C, U, F extends object = object> extends ResourceService<T, O, C, F> {
    async updateById(id: string, input: U): Promise<T> {
        console.info(`Updating ${this.path}/${id}`)
        const output = await this.requestClient.put<U, O>(`${this.path}/${id}`, input)
        console.info("Success")
        return this.from(output)
    }
}
