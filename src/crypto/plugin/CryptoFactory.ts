/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

import CryptoSuite, { CryptoSuiteMap } from './CryptoSuite';
import DefaultCryptoSuite from './DefaultCryptoSuite';
import IKeyStore from '../keyStore/IKeyStore';
import { SubtleCrypto } from 'webcrypto-core';

/**
 * Utility class to handle all CryptoSuite dependency injection
 */
export default class CryptoFactory {

  private keyEncrypters: CryptoSuiteMap;
  private sharedKeyEncrypters: CryptoSuiteMap;
  private symmetricEncrypter: CryptoSuiteMap;
  private messageSigners: CryptoSuiteMap;
  private macSigners: CryptoSuiteMap;
  private messageDigests: CryptoSuiteMap;

  public keyStore: IKeyStore;

  /**
   * Constructs a new CryptoRegistry
   * @param keyStore used to store private jeys
   * @param suite The suite to use for dependency injection
   */
  constructor (keyStore: IKeyStore, suite?: CryptoSuite) {
    this.keyStore = keyStore;
    let crypto: CryptoSuite; 
    if (suite) {
      crypto = suite;
    } else {
    // Set default API
    crypto = new DefaultCryptoSuite();
    }
    this.keyEncrypters = <CryptoSuiteMap>{'*': crypto };
    this.sharedKeyEncrypters =<CryptoSuiteMap>{'*': crypto };
    this.symmetricEncrypter = <CryptoSuiteMap>{'*': crypto };
    this.messageSigners = <CryptoSuiteMap>{'*': crypto };
    this.macSigners = <CryptoSuiteMap>{'*': crypto };
    this.messageDigests = <CryptoSuiteMap>{'*': crypto };
  }

  /**
   * Gets the key encrypter object given the encryption algorithm's name
   * @param name The name of the algorithm
   * @returns The corresponding crypto API
   */
  public getKeyEncrypter (name: string): SubtleCrypto {
    if (this.keyEncrypters[name]) {
      return this.keyEncrypters[name].getKekEncrypters();
    }
    return this.keyEncrypters['*'].getKekEncrypters();
  }

  /**
   * Gets the shared key encrypter object given the encryption algorithm's name
   * Used for DH algorithms
   * @param name The name of the algorithm
   * @returns The corresponding crypto API
   */
  getSharedKeyEncrypter (name: string): SubtleCrypto {
    if (this.sharedKeyEncrypters[name]) {
      return this.sharedKeyEncrypters[name].getSharedKeyEncrypters();
    }
    return this.sharedKeyEncrypters['*'].getSharedKeyEncrypters();
  }

  /**
   * Gets the SymmetricEncrypter object given the symmetric encryption algorithm's name
   * @param name The name of the algorithm
   * @returns The corresponding crypto API
   */
  getSymmetricEncrypter (name: string): SubtleCrypto {
    if (this.symmetricEncrypter[name]) {
      return this.symmetricEncrypter[name].getSymmetricEncrypters();
    }
    return this.symmetricEncrypter['*'].getSymmetricEncrypters();
  }
  
  /**
   * Gets the message signer object given the signing algorithm's name
   * @param name The name of the algorithm
   * @returns The corresponding crypto API
   */
  getMessageSigner (name: string): SubtleCrypto {
    if (this.messageSigners[name]) {
      return this.messageSigners[name].getMessageSigners();
    }
    return this.messageSigners['*'].getMessageSigners();
  }

  /**
   * Gets the mac signer object given the signing algorithm's name
   * @param name The name of the algorithm
   * @returns The corresponding crypto API
   */
  getMacSigner (name: string): SubtleCrypto {
    if (this.macSigners[name]) {
      return this.macSigners[name].getMacSigners();
    }
    return this.macSigners['*'].getMacSigners();
  }

  /**
   * Gets the message digest object given the digest algorithm's name
   * @param name The name of the algorithm
   * @returns The corresponding crypto API
   */
  getMessageDigest (name: string): SubtleCrypto {
    if (this.messageDigests[name]) {
      return this.messageDigests[name].getMessageDigests();
    }
    return this.messageDigests['*'].getMessageDigests();
  }
}
