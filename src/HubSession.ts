import { DidResolver } from '@decentralized-identity/did-common-typescript';
import { Authentication, PrivateKey, CryptoSuite } from '@decentralized-identity/did-auth-jose';
import HubRequest from './requests/HubRequest';
import HubError, { HubErrorCode } from './HubError';
import IHubResponse from './interfaces/IHubResponse';
import HubWriteRequest from './requests/HubWriteRequest';
import HubWriteResponse from './responses/HubWriteResponse';
import HubObjectQueryRequest from './requests/HubObjectQueryRequest';
import HubObjectQueryResponse from './responses/HubObjectQueryResponse';
import HubCommitQueryRequest from './requests/HubCommitQueryRequest';
import HubCommitQueryResponse from './responses/HubCommitQueryResponse';
import IHubError from './interfaces/IHubError';

// tslint:disable-next-line:import-name
import fetch from 'node-fetch';

/**
 * Options for instantiating a new Hub session.
 */
export interface HubSessionOptions {

  /** The DID of the client, i.e the identity of the user/app using this SDK. */
  clientDid: string;

  /**
   * The private key to use for decrypting/signing when communicating with the Hub. Must be
   * registered in the DID document of the clientDid.
   */
  clientPrivateKey: PrivateKey;

  /** The DID owning the Hub with which we will be communicating. */
  targetDid: string;

  /** The DID of the Hub, for addressing request envelopes. */
  hubDid: string;

  /** The HTTPS endpoint of the Hub. */
  hubEndpoint: string;

  /** A DID resolver instance to be used during authentication. */
  resolver: DidResolver;

  /**
   * An array of CryptoSuites to use during authentication (optional). Allows the client to provide
   * a suite which delegates operations to a secure enclave, rather than using a private key
   * available in-memory.
   */
  cryptoSuites?: CryptoSuite[];
}

/**
 * Represents a communication session with a particular Hub instance.
 */
export default class HubSession {

  private authentication: Authentication;
  private clientDid: string;
  private clientPrivateKey: PrivateKey;
  private hubDid: string;
  private hubEndpoint: string;
  private targetDid: string;

  private currentAccessToken: string | undefined;

  constructor(options: HubSessionOptions) {
    this.clientPrivateKey = options.clientPrivateKey;
    this.clientDid = options.clientDid;
    this.hubDid = options.hubDid;
    this.hubEndpoint = options.hubEndpoint;
    this.targetDid = options.targetDid;

    const kid = this.clientPrivateKey.kid;
    this.authentication = new Authentication({
      keys: { [kid] : this.clientPrivateKey },
      resolver: options.resolver,
      cryptoSuites: options.cryptoSuites,
    });
  }

  /**
   * Sends the given request to the Hub instance, and returns the associated response.
   *
   * @param request An instance or subclass of HubRequest to be sent.
   */
  send(request: HubWriteRequest): Promise<HubWriteResponse>;
  send(request: HubObjectQueryRequest): Promise<HubObjectQueryResponse>;
  send(request: HubCommitQueryRequest): Promise<HubCommitQueryResponse>;
  async send(request: HubRequest): Promise<any> {

    const rawRequestJson = await request.getRequestJson();

    rawRequestJson.iss = this.clientDid;
    rawRequestJson.aud = this.hubDid;
    rawRequestJson.sub = this.targetDid;

    const rawRequestString = JSON.stringify(rawRequestJson);
    const accessToken = await this.getAccessToken();

    let responseString: string;

    try {
      responseString = await this.makeRequest(rawRequestString, accessToken);
    } catch (e) {
      // If the access token has expired, renew access token and retry
      if (HubError.is(e) && e.getErrorCode() === HubErrorCode.AuthenticationFailed) {
        const newAccessToken = await this.refreshAccessToken();
        responseString = await this.makeRequest(rawRequestString, newAccessToken);
      } else {
        throw e;
      }
    }

    let responseObject: IHubResponse;

    try {
      responseObject = JSON.parse(responseString);
    } catch (e) {
      throw new HubError({
        error_code: HubErrorCode.ServerError,
        developer_message: `Unexpected error decoding JSON response: ${e.message}`,
        inner_error: e,
      });
    }

    return HubSession.mapResponseToObject(responseObject);
  }

  /**
   * Sends a raw (string) request body to the Hub and receives a response.
   *
   * @param message The raw request body to send.
   * @param accessToken The access token to include in the request, if any.
   */
  private async makeRequest(message: string, accessToken?: string): Promise<string> {

    const requestBuffer = await this.authentication.getAuthenticatedRequest(message, this.clientPrivateKey, this.hubDid, accessToken);

    const res = await fetch(this.hubEndpoint, {
      method: 'POST',
      body: requestBuffer,
      headers: {
        'Content-Type': 'application/jwt',
        'Content-Length': requestBuffer.length.toString(),
      },
    });

    if (res.status !== 200) {
      const errorResponse = await res.json();
      throw new HubError(errorResponse);
    }

    const response = await res.buffer();
    const plainResponse = await this.authentication.getVerifiedRequest(response, false);
    if (plainResponse instanceof Buffer) {
      // This should never happen as it means we are trying to return an access token in response
      throw new Error('Internal error during decryption.');
    }

    return plainResponse.request;
  }

  /**
   * Returns the current access token for the Hub, requesting one if necessary.
   */
  private async getAccessToken(): Promise<string> {
    if (this.currentAccessToken) {
      return this.currentAccessToken;
    }

    return this.refreshAccessToken();
  }

  /**
   * Requests an updated access token from the Hub.
   */
  private async refreshAccessToken(): Promise<string> {
    this.currentAccessToken = await this.makeRequest('');
    return this.currentAccessToken!;
  }

  /** Mapping of known response types. */
  private static responseTypes = {
    WriteResponse: HubWriteResponse,
    ObjectQueryResponse: HubObjectQueryResponse,
    CommitQueryResponse: HubCommitQueryResponse,
  };

  /**
   * Transforms a JSON blob returned by the Hub into a subclass of HubResponse, based on the `@type`
   * field of the response.
   *
   * @param response The Hub response to be transformed.
   */
  private static mapResponseToObject(response: IHubResponse) {
    const responseTypeString = response['@type'];
    const responseType = (HubSession.responseTypes as any)[responseTypeString];

    if (responseType) {
      return new responseType(response);
    }

    if (response['@type'] === 'ErrorResponse') {
      throw new HubError(response as any as IHubError);
    }

    throw new HubError({
      error_code: HubErrorCode.NotImplemented,
      developer_message: `Unexpected response type ${responseTypeString}`,
    });
  }

}
